# Deploying SharePay to AWS (single EC2 + docker-compose)

This runs the whole stack on one EC2 instance using `docker-compose.prod.yml`:

```
            ┌────────────────────────── EC2 instance ──────────────────────────┐
 Internet ─▶│  :80  frontend (nginx)  ──/api──▶  backend (Spring Boot, :8080)   │
            │                                         │                         │
            │                                    postgres (:5432, internal)     │
            └───────────────────────────────────────────────────────────────────┘
```

Only port **80** is exposed. The backend and Postgres are reachable only inside the Docker
network. nginx proxies `/api` to the backend, so the browser is always same-origin (no CORS).

> Cost note: `t3.small` is not free-tier. For free-tier use `t2.micro`/`t3.micro` (the setup
> script adds 2 GB swap so the builds still succeed, just slower). Stop the instance when idle.

---

## 0. Prerequisites

- An AWS account and the **AWS CLI** installed & configured (`aws configure`).
- An SSH client (built into Git Bash / macOS / Linux).
- Your code reachable by the instance — easiest is to **push this repo to GitHub** first.

Set a few shell variables (adjust region):

```bash
export AWS_REGION=ap-southeast-1
export KEY_NAME=sharepay-key
export SG_NAME=sharepay-sg
```

## 1. Create an SSH key pair

```bash
aws ec2 create-key-pair --region "$AWS_REGION" --key-name "$KEY_NAME" \
  --query 'KeyMaterial' --output text > "$KEY_NAME.pem"
chmod 600 "$KEY_NAME.pem"
```

## 2. Create a security group (SSH from your IP, HTTP from anywhere)

```bash
MYIP=$(curl -s https://checkip.amazonaws.com)

SG_ID=$(aws ec2 create-security-group --region "$AWS_REGION" \
  --group-name "$SG_NAME" --description "SharePay" --query 'GroupId' --output text)

# SSH (22) only from your current IP
aws ec2 authorize-security-group-ingress --region "$AWS_REGION" --group-id "$SG_ID" \
  --protocol tcp --port 22 --cidr "${MYIP}/32"

# HTTP (80) from anywhere
aws ec2 authorize-security-group-ingress --region "$AWS_REGION" --group-id "$SG_ID" \
  --protocol tcp --port 80 --cidr 0.0.0.0/0

# (Optional, only if you add HTTPS later) 443 from anywhere
# aws ec2 authorize-security-group-ingress --region "$AWS_REGION" --group-id "$SG_ID" \
#   --protocol tcp --port 443 --cidr 0.0.0.0/0
```

## 3. Launch the instance (Amazon Linux 2023, 20 GB disk)

```bash
AMI=$(aws ssm get-parameters --region "$AWS_REGION" \
  --names /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 \
  --query 'Parameters[0].Value' --output text)

INSTANCE_ID=$(aws ec2 run-instances --region "$AWS_REGION" \
  --image-id "$AMI" --instance-type t3.small \
  --key-name "$KEY_NAME" --security-group-ids "$SG_ID" \
  --block-device-mappings '[{"DeviceName":"/dev/xvda","Ebs":{"VolumeSize":20,"VolumeType":"gp3"}}]' \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=sharepay}]' \
  --query 'Instances[0].InstanceId' --output text)

aws ec2 wait instance-running --region "$AWS_REGION" --instance-ids "$INSTANCE_ID"

PUBLIC_DNS=$(aws ec2 describe-instances --region "$AWS_REGION" --instance-ids "$INSTANCE_ID" \
  --query 'Reservations[0].Instances[0].PublicDnsName' --output text)
echo "Instance: $INSTANCE_ID   URL: http://$PUBLIC_DNS"
```

## 4. Get the code onto the instance

**Option A — clone from GitHub (recommended):**

```bash
ssh -i "$KEY_NAME.pem" ec2-user@"$PUBLIC_DNS" \
  "git clone https://github.com/<you>/<your-repo>.git sharepay"
```

**Option B — copy your local folder (no GitHub needed):** from your project root:

```bash
rsync -av --exclude node_modules --exclude target --exclude .git \
  -e "ssh -i deploy/aws/$KEY_NAME.pem" ./ ec2-user@"$PUBLIC_DNS":~/sharepay/
```

## 5. Install Docker and deploy

SSH in:

```bash
ssh -i "$KEY_NAME.pem" ec2-user@"$PUBLIC_DNS"
```

Then on the instance:

```bash
cd ~/sharepay
bash deploy/aws/setup-ec2.sh      # installs Docker + Compose + swap
exit                              # log out so the 'docker' group applies
```

SSH back in and start the stack:

```bash
ssh -i "$KEY_NAME.pem" ec2-user@"$PUBLIC_DNS"
cd ~/sharepay
cp .env.prod.example .env
nano .env                         # set DB_PASSWORD and a strong JWT_SECRET (openssl rand -base64 48)
bash deploy/aws/deploy.sh         # builds images and starts everything
```

First build takes a few minutes (Maven + Vite). When it finishes, open:

```
http://<PUBLIC_DNS>
```

On first start with an empty database the demo data seeds automatically (the `docker`
profile), so you can log in with **huy@sharepay.dev / password123** — change/remove this for a
real deployment.

---

## Day-2 operations

```bash
# Logs
docker compose -f docker-compose.prod.yml logs -f backend

# Update after pulling new code
git pull && bash deploy/aws/deploy.sh

# Stop / start
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml up -d

# Stop the EC2 instance to save money (data persists on the volume)
aws ec2 stop-instances --region "$AWS_REGION" --instance-ids "$INSTANCE_ID"
```

## Optional: HTTPS

A raw EC2 public IP/DNS can't get a TLS cert. To enable HTTPS you need a **domain** pointing
at the instance, then either:

- **Caddy edge (simplest):** add a Caddy container in front that auto-provisions Let's Encrypt
  certs, proxying `:443 -> frontend:80`. Open port 443 in the security group. (Ask and I'll add
  a ready `docker-compose.https.yml` + `Caddyfile`.)
- **AWS-native:** put an Application Load Balancer in front with an ACM certificate, or use
  CloudFront. More moving parts but integrates with Route 53.

## Teardown

```bash
aws ec2 terminate-instances --region "$AWS_REGION" --instance-ids "$INSTANCE_ID"
aws ec2 wait instance-terminated --region "$AWS_REGION" --instance-ids "$INSTANCE_ID"
aws ec2 delete-security-group --region "$AWS_REGION" --group-id "$SG_ID"
aws ec2 delete-key-pair --region "$AWS_REGION" --key-name "$KEY_NAME"
```
