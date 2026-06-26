import dayjs from 'dayjs';

export function formatDate(value?: string | null): string {
  if (!value) return '—';
  return dayjs(value).format('MMM D, YYYY');
}

export function formatDateRange(start?: string | null, end?: string | null): string {
  if (!start && !end) return 'No dates set';
  if (start && end) {
    return `${dayjs(start).format('MMM D')} – ${dayjs(end).format('MMM D, YYYY')}`;
  }
  return formatDate(start ?? end);
}

export function initials(name: string): string {
  return name
    .split(/\s+/)
    .map((part) => part.charAt(0))
    .filter(Boolean)
    .slice(0, 2)
    .join('')
    .toUpperCase();
}
