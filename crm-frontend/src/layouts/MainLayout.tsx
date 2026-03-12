/* ============================================================
   MainLayout – sidebar + topbar + content area
   ============================================================ */
import React from 'react';
import { Outlet } from 'react-router-dom';
import { Box, Toolbar, useMediaQuery, useTheme } from '@mui/material';
import Sidebar, { DRAWER_WIDTH } from './Sidebar';
import TopBar from './TopBar';
import { useAppSelector, useAppDispatch } from '../hooks/useRedux';
import { setSidebarOpen } from '../store/slices/uiSlice';

const MainLayout: React.FC = () => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const { sidebarOpen } = useAppSelector((s) => s.ui);
  const dispatch = useAppDispatch();

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      {/* Sidebar: permanent on desktop, temporary on mobile */}
      <Sidebar
        open={isMobile ? sidebarOpen : true}
        variant={isMobile ? 'temporary' : 'permanent'}
        onClose={() => dispatch(setSidebarOpen(false))}
      />

      {/* Main content area */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          backgroundColor: 'background.default',
          display: 'flex',
          flexDirection: 'column',
          minHeight: '100vh',
        }}
      >
        <TopBar />
        <Toolbar /> {/* spacer for fixed AppBar */}
        <Box sx={{ p: { xs: 2, md: 3 }, flex: 1, overflow: 'auto' }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
};

export default MainLayout;
