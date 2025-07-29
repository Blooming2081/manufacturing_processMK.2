import './AlertToast.css';

const AlertToast = ({ alerts, onRemove }) => {
  const getAlertIcon = (alertType) => {
    switch (alertType) {
      case 'TEMPERATURE':
        return (
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M14 4v10.54a4 4 0 1 1-4 0V4a2 2 0 0 1 4 0Z"/>
          </svg>
        );
      case 'HUMIDITY':
        return (
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M7 16.3c2.2 0 4-1.83 4-4.05 0-1.16-.57-2.26-1.71-3.19S7.29 6.75 7 5.3c-.29 1.45-1.14 2.84-2.29 3.76S3 11.1 3 12.25c0 2.22 1.8 4.05 4 4.05z"/>
            <path d="M12.56 6.6A10.97 10.97 0 0 0 14 3.02c.5 2.5 2.04 4.6 4.14 6.36S22 12.25 22 14.5c0 2.22-1.8 4.05-4 4.05-.23 0-.44-.02-.65-.06"/>
          </svg>
        );
      case 'AIR_QUALITY':
        return (
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M8 19a4 4 0 0 1-4-4V9a4 4 0 0 1 4-4h1l2-3h2l2 3h1a4 4 0 0 1 4 4v6a4 4 0 0 1-4 4"/>
            <circle cx="12" cy="13" r="3"/>
          </svg>
        );
      default:
        return (
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M3 12a9 9 0 1 0 18 0a9 9 0 0 0 -18 0"/>
            <path d="M12 8v4"/>
            <path d="M12 16h.01"/>
          </svg>
        );
    }
  };

  if (!alerts || alerts.length === 0) return null;

  return (
    <div className="alert-toast-container">
      {alerts.map((alert) => (
        <div 
          key={alert.id} 
          className={`alert alert-${alert.type} alert-dismissible alert-toast`} 
          role="alert"
        >
          <div className="alert-icon">
            {getAlertIcon(alert.alertType)}
          </div>
          <div className="alert-content">
            <strong>{alert.alertType?.replace('_', ' ')}</strong>
            <div>{alert.message}</div>
            <small className="text-muted">
              {alert.companyName} • {new Date(alert.timestamp).toLocaleTimeString()}
            </small>
          </div>
          <button 
            type="button" 
            className="btn-close" 
            onClick={() => onRemove(alert.id)}
            aria-label="close"
          />
        </div>
      ))}
    </div>
  );
};

export default AlertToast;