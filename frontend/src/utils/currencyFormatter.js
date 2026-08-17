export const formatCurrency = (value) => {
  if (value === undefined || value === null) {
    return 'R$ 0,00';
  }

  // Handle string values
  const numValue = typeof value === 'string' ? parseFloat(value) : value;

  if (isNaN(numValue)) {
    return 'R$ 0,00';
  }

  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(numValue);
};

export const parseCurrency = (value) => {
  if (!value) return 0;
  const cleanValue = value
    .replace('R$', '')
    .replace(/\./g, '')
    .replace(',', '.')
    .trim();
  return parseFloat(cleanValue) || 0;
};