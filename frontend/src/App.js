import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { useAuth } from './hooks/useAuth';
import Login from './components/Auth/Login';
import Register from './components/Auth/Register';
import Dashboard from './components/Dashboard/Dashboard';
import WalletOverview from './components/Wallet/WalletOverview';
import TransferForm from './components/Wallet/TransferForm';
import TransactionList from './components/Transactions/TransactionList';
import Navbar from './components/Common/Navbar';
import './App.css';

const PrivateRoute = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return <div className="loading-container"><div className="loading-spinner"></div></div>;
  }

  return isAuthenticated ? children : <Navigate to="/login" />;
};

function App() {
  return (
    <AuthProvider>
      <div className="app">
        <Navbar />
        <div className="app-content">
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/" element={
              <PrivateRoute>
                <Dashboard />
              </PrivateRoute>
            } />
            <Route path="/wallet" element={
              <PrivateRoute>
                <WalletOverview />
              </PrivateRoute>
            } />
            <Route path="/transfer" element={
              <PrivateRoute>
                <TransferForm />
              </PrivateRoute>
            } />
            <Route path="/transactions" element={
              <PrivateRoute>
                <TransactionList />
              </PrivateRoute>
            } />
          </Routes>
        </div>
      </div>
    </AuthProvider>
  );
}

export default App;