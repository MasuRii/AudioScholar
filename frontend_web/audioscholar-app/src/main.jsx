import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './components/App/App.jsx'
import './styles/index.css'
import { initLogger } from './utils/logger'

// Initialize logging configuration (suppress logs if not localhost)
initLogger();

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)