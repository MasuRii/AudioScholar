import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './components/App/App.jsx'
import './styles/index.css'

if (import.meta && import.meta.env && import.meta.env.PROD) {
  const noop = () => {}
  console.log = noop
  console.info = noop
  console.debug = noop
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)