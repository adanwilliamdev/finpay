import React from 'react';
import { Box, CircularProgress } from '@mui/material';

const LoadingSpinner = ({ fullScreen = false }) => {
  return (
    <Box
      display="flex"
      justifyContent="center"
      alignItems="center"
      minHeight={fullScreen ? '100vh' : '200px'}
    >
      <CircularProgress />
    </Box>
  );
};

export default LoadingSpinner;