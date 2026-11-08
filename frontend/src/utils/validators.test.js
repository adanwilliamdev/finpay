import { isValidAmount, isValidDocument, isValidEmail, isValidPassword } from './validators';

describe('validators', () => {
  it('validates e-mails', () => {
    expect(isValidEmail('user@finpay.com')).toBe(true);
    expect(isValidEmail('not-an-email')).toBe(false);
    expect(isValidEmail('missing@tld')).toBe(false);
  });

  it('requires a strong password (same rule as the backend)', () => {
    expect(isValidPassword('User@123')).toBe(true);
    expect(isValidPassword('short1A@')).toBe(true);
    expect(isValidPassword('alllowercase1@')).toBe(false);
    expect(isValidPassword('NoDigits@@')).toBe(false);
    expect(isValidPassword('NoSpecial123')).toBe(false);
    expect(isValidPassword('Sh0r@')).toBe(false);
  });

  it('accepts CPF (11) or CNPJ (14) digits, and an empty document', () => {
    expect(isValidDocument('')).toBe(true);
    expect(isValidDocument('52998224725')).toBe(true);
    expect(isValidDocument('12345678000199')).toBe(true);
    expect(isValidDocument('1234567')).toBe(false);
    expect(isValidDocument('abcdefghijk')).toBe(false);
  });

  it('accepts only positive amounts', () => {
    expect(isValidAmount('10.50')).toBe(true);
    expect(isValidAmount('0')).toBe(false);
    expect(isValidAmount('-5')).toBe(false);
    expect(isValidAmount('abc')).toBe(false);
    expect(isValidAmount('')).toBe(false);
  });
});
