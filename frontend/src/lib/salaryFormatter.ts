/**
 * Salary formatting utilities for locale-aware display.
 * Uses Intl.NumberFormat for proper currency formatting.
 */

export type SalaryPeriod = 'HOUR' | 'DAY' | 'MONTH' | 'YEAR';
export type SalarySource = 'JOB_POSTING' | 'ATS_API' | 'MARKET_DATA' | 'AI_ESTIMATE';
export type ConfidenceLevel = 'HIGH' | 'MEDIUM' | 'LOW';

export interface NormalizedSalary {
  amount: number; // In cents (e.g., 12000000 = $120,000)
  currency: string; // ISO 4217 code (NZD, AUD, USD, EUR)
  period: SalaryPeriod;
  source: SalarySource;
  isGross?: boolean;
  disclaimer?: string | null; // Computed at BFF level based on source
}

/**
 * Get the confidence level based on salary source.
 */
export function getConfidenceLevel(source: SalarySource): ConfidenceLevel {
  switch (source) {
    case 'JOB_POSTING':
    case 'ATS_API':
      return 'HIGH';
    case 'MARKET_DATA':
      return 'MEDIUM';
    case 'AI_ESTIMATE':
      return 'LOW';
    default:
      return 'LOW';
  }
}

/**
 * Format a salary amount as a localized currency string.
 * @param amountCents Amount in cents (e.g., 12000000 for $120,000)
 * @param currency ISO 4217 currency code
 * @param locale Locale string (default: browser locale)
 * @returns Formatted currency string (e.g., "$120,000")
 */
export function formatSalaryAmount(
  amountCents: number,
  currency: string,
  locale?: string
): string {
  const amount = amountCents / 100; // Convert cents to dollars
  
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(amount);
}

/**
 * Format the salary period for display.
 * @param period Salary period code
 * @returns Human-readable period string
 */
export function formatSalaryPeriod(period: SalaryPeriod): string {
  switch (period) {
    case 'HOUR':
      return 'per hour';
    case 'DAY':
      return 'per day';
    case 'MONTH':
      return 'per month';
    case 'YEAR':
      return 'per year';
    default:
      return '';
  }
}

/**
 * Format a complete salary for display.
 * @param salary NormalizedSalary object
 * @param locale Locale string (default: browser locale)
 * @returns Formatted salary string (e.g., "$120,000 per year")
 */
export function formatSalary(
  salary: NormalizedSalary,
  locale?: string
): string {
  const amount = formatSalaryAmount(salary.amount, salary.currency, locale);
  const period = formatSalaryPeriod(salary.period);
  return `${amount} ${period}`;
}

/**
 * Format a salary range for display.
 * @param min Minimum salary
 * @param max Maximum salary
 * @param locale Locale string (default: browser locale)
 * @returns Formatted range string (e.g., "$120,000 - $150,000 per year")
 */
export function formatSalaryRange(
  min: NormalizedSalary,
  max: NormalizedSalary,
  locale?: string
): string {
  // If both salaries have the same currency and period, show as a range
  if (min.currency === max.currency && min.period === max.period) {
    const minAmount = formatSalaryAmount(min.amount, min.currency, locale);
    const maxAmount = formatSalaryAmount(max.amount, max.currency, locale);
    const period = formatSalaryPeriod(min.period);
    return `${minAmount} - ${maxAmount} ${period}`;
  }
  
  // Different currencies or periods, show separately
  return `${formatSalary(min, locale)} - ${formatSalary(max, locale)}`;
}

/**
 * Get a confidence badge color based on salary source.
 * @param source Salary source
 * @returns Tailwind CSS color classes
 */
export function getConfidenceBadgeClasses(source: SalarySource): string {
  const confidence = getConfidenceLevel(source);
  switch (confidence) {
    case 'HIGH':
      return 'bg-green-100 text-green-800 border-green-300';
    case 'MEDIUM':
      return 'bg-yellow-100 text-yellow-800 border-yellow-300';
    case 'LOW':
      return 'bg-orange-100 text-orange-800 border-orange-300';
    default:
      return 'bg-gray-100 text-gray-800 border-gray-300';
  }
}

/**
 * Get a confidence badge label.
 * @param source Salary source
 * @returns Human-readable confidence label
 */
export function getConfidenceLabel(source: SalarySource): string {
  const confidence = getConfidenceLevel(source);
  switch (confidence) {
    case 'HIGH':
      return 'Verified';
    case 'MEDIUM':
      return 'Estimated';
    case 'LOW':
      return 'AI Estimate';
    default:
      return 'Unknown';
  }
}

/**
 * Convert a salary to annual amount in the same currency.
 * Uses standard assumptions:
 * - Hourly: 2080 hours/year (40 hrs/week × 52 weeks)
 * - Daily: 260 days/year (5 days/week × 52 weeks)
 * - Monthly: 12 months/year
 * - Yearly: already annual
 */
export function toAnnualAmount(salary: NormalizedSalary): number {
  switch (salary.period) {
    case 'HOUR':
      return salary.amount * 2080;
    case 'DAY':
      return salary.amount * 260;
    case 'MONTH':
      return salary.amount * 12;
    case 'YEAR':
      return salary.amount;
    default:
      return salary.amount;
  }
}

/**
 * Compare two salaries by converting to annual amount.
 * Returns negative if a < b, positive if a > b, 0 if equal.
 * Note: This doesn't account for currency differences.
 */
export function compareSalaries(a: NormalizedSalary, b: NormalizedSalary): number {
  if (a.currency !== b.currency) {
    console.warn('Comparing salaries with different currencies:', a.currency, b.currency);
  }
  return toAnnualAmount(a) - toAnnualAmount(b);
}
