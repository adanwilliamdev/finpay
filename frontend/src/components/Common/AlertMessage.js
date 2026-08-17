import React from 'react';
import { Alert, Box } from '@mui/material';

const AlertMessage = ({ type, message, onClose }) => {
  if (!message) return null;

  return (
    <Box sx={{ mb: 2 }}>
      <Alert severity={type} onClose={onClose}>
        {message}
      </Alert>
    </Box>
  );
};

export default AlertMessage;