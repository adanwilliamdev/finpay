import React, { useState } from 'react';
import { Paper, Typography, Box, IconButton, Divider } from '@mui/material';
import { Visibility, VisibilityOff, ArrowUpward, ArrowDownward } from '@mui/icons-material';
import { formatCurrency } from '../../utils/currencyFormatter';

/**
 * A financial stat card.
 *
 * Props:
 * - title: small label at the top
 * - balance: numeric value to format as currency
 * - icon: MUI icon element
 * - color: accent color for the icon chip and trend
 * - subtitle: optional secondary line shown below a divider (e.g. "Available balance")
 * - trend: optional { direction: 'up' | 'down', label: string } for a growth indicator
 * - maskable: if true, shows an eye toggle to hide/reveal the value
 * - isCount: if true, the value is a plain count (e.g. number of transactions) and is not
 *   formatted as currency
 */
const BalanceCard = ({ balance, title, icon, color = '#2563EB', subtitle, trend, maskable = false, isCount = false }) => {
  const [hidden, setHidden] = useState(false);

  const displayValue = hidden ? '••••••' : isCount ? balance : formatCurrency(balance);

  return (
    <Paper
      elevation={0}
      sx={{
        p: 2.75,
        height: '100%',
        minHeight: 152,
        display: 'flex',
        flexDirection: 'column',
        borderRadius: '16px',
        border: '1px solid #E2E8F0',
        boxShadow: '0 1px 2px rgba(15, 23, 42, 0.06)',
        transition: 'box-shadow 0.2s ease, transform 0.2s ease',
        '&:hover': {
          boxShadow: '0 8px 20px rgba(15, 23, 42, 0.08)',
          transform: 'translateY(-1px)',
        },
      }}
    >
      <Box display="flex" justifyContent="space-between" alignItems="flex-start">
        <Typography
          variant="body2"
          sx={{ color: '#64748B', fontWeight: 600, fontSize: '0.82rem' }}
        >
          {title}
        </Typography>

        {maskable ? (
          <IconButton
            size="small"
            onClick={() => setHidden((v) => !v)}
            sx={{ color: '#94A3B8', mt: -0.5, mr: -0.5 }}
          >
            {hidden ? <VisibilityOff sx={{ fontSize: 18 }} /> : <Visibility sx={{ fontSize: 18 }} />}
          </IconButton>
        ) : (
          <Box
            sx={{
              backgroundColor: color + '1A',
              borderRadius: '10px',
              width: 36,
              height: 36,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color,
              flexShrink: 0,
            }}
          >
            {icon}
          </Box>
        )}
      </Box>

      <Typography
        variant="h4"
        sx={{
          mt: 1.5,
          fontWeight: 700,
          fontSize: '1.65rem',
          color: '#0F172A',
          letterSpacing: '-0.02em',
        }}
      >
        {displayValue}
      </Typography>

      <Box sx={{ mt: 'auto', pt: 1.5 }}>
        {trend && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            {trend.direction === 'up' ? (
              <ArrowUpward sx={{ fontSize: 14, color: '#16A34A' }} />
            ) : (
              <ArrowDownward sx={{ fontSize: 14, color: '#DC2626' }} />
            )}
            <Typography
              variant="caption"
              sx={{
                fontWeight: 700,
                color: trend.direction === 'up' ? '#16A34A' : '#DC2626',
              }}
            >
              {trend.label}
            </Typography>
          </Box>
        )}

        {subtitle && (
          <>
            <Divider sx={{ my: 1, borderColor: '#E2E8F0' }} />
            <Box display="flex" justifyContent="space-between" alignItems="center">
              <Typography variant="caption" sx={{ color: '#64748B', fontWeight: 500 }}>
                {subtitle.label}
              </Typography>
              <Typography variant="caption" sx={{ color: '#0F172A', fontWeight: 700 }}>
                {hidden ? '••••••' : formatCurrency(subtitle.value)}
              </Typography>
            </Box>
          </>
        )}
      </Box>
    </Paper>
  );
};

export default BalanceCard;
