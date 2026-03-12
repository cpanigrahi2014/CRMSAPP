/* ============================================================
   UI Slice – theme mode, sidebar state, global search
   ============================================================ */
import { createSlice, PayloadAction } from '@reduxjs/toolkit';

type ThemeMode = 'light' | 'dark';

interface UIState {
  themeMode: ThemeMode;
  sidebarOpen: boolean;
  globalSearch: string;
  notifications: number;
}

const initialState: UIState = {
  themeMode: (localStorage.getItem('themeMode') as ThemeMode) || 'light',
  sidebarOpen: true,
  globalSearch: '',
  notifications: 0,
};

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    toggleTheme(state) {
      state.themeMode = state.themeMode === 'light' ? 'dark' : 'light';
      localStorage.setItem('themeMode', state.themeMode);
    },
    toggleSidebar(state) {
      state.sidebarOpen = !state.sidebarOpen;
    },
    setSidebarOpen(state, action: PayloadAction<boolean>) {
      state.sidebarOpen = action.payload;
    },
    setGlobalSearch(state, action: PayloadAction<string>) {
      state.globalSearch = action.payload;
    },
    setNotifications(state, action: PayloadAction<number>) {
      state.notifications = action.payload;
    },
  },
});

export const { toggleTheme, toggleSidebar, setSidebarOpen, setGlobalSearch, setNotifications } = uiSlice.actions;
export default uiSlice.reducer;
