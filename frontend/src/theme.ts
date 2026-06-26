import { createTheme } from '@mui/material/styles';

// Indigo/violet primary with rounded, soft-shadowed cards — matching the mockup.
export const theme = createTheme({
  palette: {
    primary: { main: '#5b5bd6', dark: '#4a4ac4', light: '#7b7be0' },
    secondary: { main: '#7c3aed' },
    success: { main: '#16a34a' },
    error: { main: '#dc2626' },
    background: { default: '#f5f6fa', paper: '#ffffff' },
    text: { primary: '#1f2430', secondary: '#6b7280' },
  },
  shape: { borderRadius: 12 },
  typography: {
    fontFamily: '"Inter", "Segoe UI", system-ui, -apple-system, sans-serif',
    h4: { fontWeight: 700 },
    h5: { fontWeight: 700 },
    h6: { fontWeight: 700 },
    subtitle1: { fontWeight: 600 },
    button: { textTransform: 'none', fontWeight: 600 },
  },
  components: {
    MuiCard: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: { border: '1px solid #eceef3', borderRadius: 16 },
      },
    },
    MuiPaper: { styleOverrides: { rounded: { borderRadius: 16 } } },
    MuiButton: { defaultProps: { disableElevation: true } },
    MuiChip: { styleOverrides: { root: { fontWeight: 600 } } },
  },
});
