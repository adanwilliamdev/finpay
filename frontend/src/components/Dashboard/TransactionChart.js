import React from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { Paper, Typography, Box } from '@mui/material';
import { formatCurrency } from '../../utils/currencyFormatter';

const TransactionChart = ({ data }) => {
  if (!data || data.length === 0) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height={300}>
        <Typography color="textSecondary">
          No transaction data available
        </Typography>
      </Box>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={data}>
        <CartesianGrid strokeDasharray="3 3" stroke="#E2E8F0" />
        <XAxis dataKey="date" tick={{ fill: '#64748B', fontSize: 12 }} />
        <YAxis
          tickFormatter={(value) => formatCurrency(value)}
          tick={{ fill: '#64748B', fontSize: 12 }}
        />
        <Tooltip
          formatter={(value) => formatCurrency(value)}
          labelStyle={{ color: '#0F172A' }}
          contentStyle={{ borderRadius: 10, border: '1px solid #E2E8F0' }}
        />
        <Legend />
        <Line
          type="monotone"
          dataKey="amount"
          stroke="#2563EB"
          strokeWidth={2}
          dot={{ fill: '#2563EB' }}
          activeDot={{ r: 8 }}
        />
      </LineChart>
    </ResponsiveContainer>
  );
};

export default TransactionChart;