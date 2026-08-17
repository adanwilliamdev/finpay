import React, { useState } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import {
  AppBar,
  Toolbar,
  Typography,
  Button,
  IconButton,
  Menu,
  MenuItem,
  Box,
  Avatar,
  Divider,
  Badge,
} from '@mui/material';
import {
  AccountCircle,
  ExitToApp,
  DashboardOutlined,
  AccountBalanceWalletOutlined,
  ReceiptLongOutlined,
  NotificationsNoneOutlined,
  KeyboardArrowDown,
  AccountBalanceWallet,
} from '@mui/icons-material';

const NAV_LINKS = [
  { label: 'Dashboard', to: '/', icon: <DashboardOutlined fontSize="small" /> },
  { label: 'Wallet', to: '/wallet', icon: <AccountBalanceWalletOutlined fontSize="small" /> },
  { label: 'Transactions', to: '/transactions', icon: <ReceiptLongOutlined fontSize="small" /> },
];

const Logo = () => (
  <Box
    component={Link}
    to="/"
    sx={{
      display: 'flex',
      alignItems: 'center',
      gap: 1,
      textDecoration: 'none',
      mr: 4,
    }}
  >
    <Box
      sx={{
        width: 34,
        height: 34,
        borderRadius: '10px',
        background: 'linear-gradient(135deg, #2563EB 0%, #7C3AED 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: '#FFFFFF',
        flexShrink: 0,
      }}
    >
      <AccountBalanceWallet sx={{ fontSize: 19 }} />
    </Box>
    <Typography
      variant="h6"
      sx={{ color: '#0F172A', fontWeight: 800, letterSpacing: '-0.02em', fontSize: '1.15rem' }}
    >
      FinPay
    </Typography>
  </Box>
);

const Navbar = () => {
  const { user, logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [anchorEl, setAnchorEl] = useState(null);

  const handleMenu = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = async () => {
    await logout();
    navigate('/login');
    handleClose();
  };

  const handleProfile = () => {
    // Navigate to profile page
    handleClose();
  };

  const barSx = {
    backgroundColor: 'rgba(255, 255, 255, 0.8)',
    backdropFilter: 'blur(12px)',
    WebkitBackdropFilter: 'blur(12px)',
    borderBottom: '1px solid #E2E8F0',
    color: '#0F172A',
  };

  if (!isAuthenticated) {
    return (
      <AppBar position="static" elevation={0} sx={barSx}>
        <Toolbar sx={{ minHeight: 64 }}>
          <Box sx={{ flexGrow: 1, display: 'flex' }}>
            <Logo />
          </Box>
          <Button component={Link} to="/login" sx={{ color: '#475569', mr: 1 }}>
            Login
          </Button>
          <Button variant="contained" component={Link} to="/register" disableElevation>
            Register
          </Button>
        </Toolbar>
      </AppBar>
    );
  }

  const isActive = (path) => location.pathname === path;

  return (
    <AppBar position="static" elevation={0} sx={barSx}>
      <Toolbar sx={{ minHeight: 64 }}>
        <Logo />

        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, flexGrow: 1 }}>
          {NAV_LINKS.map((link) => {
            const active = isActive(link.to);
            return (
              <Button
                key={link.to}
                component={Link}
                to={link.to}
                startIcon={link.icon}
                sx={{
                  px: 1.75,
                  py: 0.75,
                  borderRadius: '10px',
                  fontSize: '0.9rem',
                  fontWeight: 600,
                  color: active ? '#2563EB' : '#64748B',
                  backgroundColor: active ? 'rgba(37, 99, 235, 0.08)' : 'transparent',
                  '&:hover': {
                    backgroundColor: active ? 'rgba(37, 99, 235, 0.12)' : 'rgba(15, 23, 42, 0.04)',
                  },
                }}
              >
                {link.label}
              </Button>
            );
          })}
        </Box>

        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
          <IconButton
            size="medium"
            sx={{
              color: '#64748B',
              '&:hover': { backgroundColor: 'rgba(15, 23, 42, 0.04)' },
            }}
          >
            <Badge variant="dot" color="secondary" overlap="circular">
              <NotificationsNoneOutlined fontSize="small" />
            </Badge>
          </IconButton>

          <Box
            onClick={handleMenu}
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: 0.5,
              cursor: 'pointer',
              px: 0.75,
              py: 0.5,
              borderRadius: '10px',
              ml: 0.5,
              '&:hover': { backgroundColor: 'rgba(15, 23, 42, 0.04)' },
            }}
          >
            <Avatar
              sx={{
                width: 32,
                height: 32,
                fontSize: '0.9rem',
                fontWeight: 700,
                background: 'linear-gradient(135deg, #2563EB 0%, #7C3AED 100%)',
              }}
            >
              {user?.fullName?.charAt(0).toUpperCase() || 'U'}
            </Avatar>
            <KeyboardArrowDown sx={{ fontSize: 18, color: '#94A3B8' }} />
          </Box>

          <Menu
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleClose}
            PaperProps={{
              sx: {
                mt: 1,
                borderRadius: '12px',
                border: '1px solid #E2E8F0',
                boxShadow: '0 8px 24px rgba(15, 23, 42, 0.08)',
                minWidth: 200,
              },
            }}
          >
            <MenuItem disabled sx={{ opacity: '1 !important' }}>
              <Typography variant="body2" sx={{ fontWeight: 600, color: '#0F172A' }}>
                {user?.fullName || 'User'}
              </Typography>
            </MenuItem>
            <MenuItem disabled sx={{ opacity: '1 !important', mt: -1 }}>
              <Typography variant="caption" sx={{ color: '#64748B' }}>
                {user?.email || ''}
              </Typography>
            </MenuItem>
            <Divider sx={{ my: 0.5 }} />
            <MenuItem onClick={handleProfile} sx={{ color: '#334155', fontSize: '0.9rem' }}>
              <AccountCircle sx={{ mr: 1.5, fontSize: 20 }} /> Profile
            </MenuItem>
            <MenuItem onClick={handleLogout} sx={{ color: '#DC2626', fontSize: '0.9rem' }}>
              <ExitToApp sx={{ mr: 1.5, fontSize: 20 }} /> Logout
            </MenuItem>
          </Menu>
        </Box>
      </Toolbar>
    </AppBar>
  );
};

export default Navbar;
