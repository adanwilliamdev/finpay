import React from 'react';
import { Paper, Typography, Box } from '@mui/material';
import { formatCurrency } from '../../utils/currencyFormatter';

const BalanceCard = ({ balance, title, icon, color }) => {
  return (
    <Paper
      sx={{
        p: 3,
        display: 'flex',
        flexDirection: 'column',
        height: 140,
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      <Box display="flex" justifyContent="space-between" alignItems="center">
        <Typography variant="body2" color="textSecondary">
          {title}
        </Typography>
        <Box
          sx={{
            backgroundColor: color + '20',
            borderRadius: '50%',
            width: 40,
            height: 40,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: color,
          }}
        >
          {icon}
        </Box>
      </Box>
      <Typography variant="h4" sx={{ mt: 'auto', fontWeight: 500 }}>
        {formatCurrency(balance)}
      </Typography>
    </Paper>
  );
};

export default BalanceCard;