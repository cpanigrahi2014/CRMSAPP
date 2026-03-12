/* ============================================================
   TopBar – header with search, notifications, user profile
   ============================================================ */
import React from 'react';
import {
  AppBar,
  Toolbar,
  IconButton,
  InputBase,
  Badge,
  Avatar,
  Box,
  Tooltip,
  Menu,
  MenuItem,
  Typography,
  useTheme,
} from '@mui/material';
import {
  Menu as MenuIcon,
  Search as SearchIcon,
  Notifications as NotifIcon,
  DarkMode,
  LightMode,
  Logout,
  Person,
} from '@mui/icons-material';
import { useAppDispatch, useAppSelector } from '../hooks/useRedux';
import { toggleTheme, toggleSidebar, setGlobalSearch } from '../store/slices/uiSlice';
import { logout } from '../store/slices/authSlice';
import { useNavigate, useLocation } from 'react-router-dom';
import { DRAWER_WIDTH } from './Sidebar';

const SEARCH_PAGES = ['/dashboard', '/leads', '/accounts', '/contacts', '/opportunities', '/activities'];

const TopBar: React.FC = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const theme = useTheme();
  const showSearch = SEARCH_PAGES.some((p) => location.pathname.startsWith(p));
  const { themeMode, globalSearch, notifications, sidebarOpen } = useAppSelector((s) => s.ui);
  const { user } = useAppSelector((s) => s.auth);

  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);

  const handleLogout = () => {
    setAnchorEl(null);
    dispatch(logout());
    navigate('/login');
  };

  return (
    <AppBar
      position="fixed"
      elevation={0}
      sx={{
        width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
        ml: { md: `${DRAWER_WIDTH}px` },
        transition: theme.transitions.create(['margin', 'width'], {
          easing: theme.transitions.easing.sharp,
          duration: theme.transitions.duration.leavingScreen,
        }),
        backgroundColor: theme.palette.background.paper,
        borderBottom: `1px solid ${theme.palette.divider}`,
        color: theme.palette.text.primary,
      }}
    >
      <Toolbar sx={{ gap: 1 }}>
        <IconButton onClick={() => dispatch(toggleSidebar())} edge="start" sx={{ display: { md: 'none' } }}>
          <MenuIcon />
        </IconButton>

        {/* Global search */}
        {showSearch && (
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            backgroundColor: theme.palette.mode === 'light' ? '#f1f5f9' : '#334155',
            borderRadius: 2,
            px: 2,
            py: 0.5,
            flex: 1,
            maxWidth: 480,
          }}
        >
          <SearchIcon sx={{ color: 'text.secondary', mr: 1 }} />
          <InputBase
            placeholder="Search leads, contacts, accounts…"
            value={globalSearch}
            onChange={(e) => dispatch(setGlobalSearch(e.target.value))}
            sx={{ flex: 1, fontSize: 14 }}
          />
        </Box>
        )}

        <Box sx={{ flexGrow: 1 }} />

        {/* Theme toggle */}
        <Tooltip title={themeMode === 'light' ? 'Dark mode' : 'Light mode'}>
          <IconButton onClick={() => dispatch(toggleTheme())}>
            {themeMode === 'light' ? <DarkMode /> : <LightMode />}
          </IconButton>
        </Tooltip>

        {/* Notifications */}
        <Tooltip title="Notifications">
          <IconButton>
            <Badge badgeContent={notifications} color="error">
              <NotifIcon />
            </Badge>
          </IconButton>
        </Tooltip>

        {/* User avatar menu */}
        <Tooltip title="Account">
          <IconButton onClick={(e) => setAnchorEl(e.currentTarget)}>
            <Avatar sx={{ width: 34, height: 34, bgcolor: 'primary.main', fontSize: 14 }}>
              {user ? `${user.firstName[0]}${user.lastName[0]}` : 'U'}
            </Avatar>
          </IconButton>
        </Tooltip>

        <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
          <Box sx={{ px: 2, py: 1 }}>
            <Typography variant="subtitle2">{user ? `${user.firstName} ${user.lastName}` : 'User'}</Typography>
            <Typography variant="caption" color="text.secondary">{user?.email}</Typography>
          </Box>
          <MenuItem onClick={() => { setAnchorEl(null); navigate('/settings'); }}>
            <Person fontSize="small" sx={{ mr: 1 }} /> Profile
          </MenuItem>
          <MenuItem onClick={handleLogout}>
            <Logout fontSize="small" sx={{ mr: 1 }} /> Logout
          </MenuItem>
        </Menu>
      </Toolbar>
    </AppBar>
  );
};

export default TopBar;
