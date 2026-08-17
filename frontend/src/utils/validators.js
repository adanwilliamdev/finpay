export const isValidEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

export const isValidPassword = (password) => {
  // At least 8 characters, one uppercase, one lowercase, one number, one special character
  const passwordRegex = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).{8,}$/;
  return passwordRegex.test(password);
};

export const isValidDocument = (document) => {
  if (!document) return true;
  // CPF: 11 digits, CNPJ: 14 digits
  const documentRegex = /^\d{11}$|^\d{14}$/;
  return documentRegex.test(document);
};

export const isValidAmount = (amount) => {
  if (!amount) return false;
  const numAmount = parseFloat(amount);
  return !isNaN(numAmount) && numAmount > 0;
};