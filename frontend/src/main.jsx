import React from 'react'
import ReactDOM from 'react-dom/client'
import '@carbon/styles/css/styles.css'
import CarbonApp from './components/CarbonApp.jsx'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <CarbonApp />
  </React.StrictMode>,
)
