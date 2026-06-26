#!/usr/bin/env bash
#
# One-time bootstrap for a fresh Amazon Linux 2023 EC2 instance.
# Installs Docker + the Compose plugin and adds a swap file so the in-container
# Maven/Vite builds don't run out of memory on small (1-2 GB) instances.
#
# Usage (on the instance):  bash deploy/aws/setup-ec2.sh
set -euo pipefail

echo ">> Installing Docker and git..."
sudo dnf update -y
sudo dnf install -y docker git
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"

echo ">> Installing the Docker Compose plugin..."
DOCKER_CLI_PLUGINS=/usr/local/lib/docker/cli-plugins
COMPOSE_VERSION=v2.29.7
sudo mkdir -p "$DOCKER_CLI_PLUGINS"
sudo curl -fsSL \
  "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-linux-$(uname -m)" \
  -o "$DOCKER_CLI_PLUGINS/docker-compose"
sudo chmod +x "$DOCKER_CLI_PLUGINS/docker-compose"

echo ">> Adding a 2 GB swap file (idempotent)..."
if [ ! -f /swapfile ]; then
  sudo dd if=/dev/zero of=/swapfile bs=1M count=2048 status=none
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
fi

echo ""
echo ">> Done. Versions:"
docker --version
sudo docker compose version
echo ""
echo ">> IMPORTANT: log out and back in (or run 'newgrp docker') so the 'docker' group"
echo "   membership takes effect, then run deploy/aws/deploy.sh"
