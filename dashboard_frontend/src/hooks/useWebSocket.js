import { useEffect, useRef, useState } from 'react';

// 웹소켓 라이브러리를 동적으로 로드하여 에러 방지
let SockJS, Stomp;

const loadWebSocketLibraries = async () => {
  try {
    if (!SockJS) {
      const sockjsModule = await import('sockjs-client');
      SockJS = sockjsModule.default;
    }
    if (!Stomp) {
      const stompModule = await import('@stomp/stompjs');
      Stomp = stompModule.Client;
    }
    return { SockJS, Stomp };
  } catch (error) {
    console.error('웹소켓 라이브러리 로드 오류:', error);
    return null;
  }
};

const useWebSocket = (companyName, userId) => {
  const stompClient = useRef(null);
  const [isConnected, setIsConnected] = useState(false);
  const [alerts, setAlerts] = useState([]);
  const [realtimeData, setRealtimeData] = useState(null);

  useEffect(() => {
    if (!companyName || !userId) return;

    const initWebSocket = async () => {
      const libs = await loadWebSocketLibraries();
      if (!libs) {
        console.error('웹소켓 라이브러리 로드 실패');
        return;
      }

      try {
        // WebSocket 연결 설정
        stompClient.current = new libs.Stomp();
        stompClient.current.webSocketFactory = () => {
          return new libs.SockJS(`${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/ws`);
        };

        // 연결 콜백 설정
        stompClient.current.onConnect = (frame) => {
          console.log('WebSocket 연결됨:', frame);
          setIsConnected(true);

          // 회사별 알림 구독
          const alertTopic = `/topic/alerts/${companyName}`;
          stompClient.current.subscribe(alertTopic, (message) => {
            const alert = JSON.parse(message.body);
            console.log('새 알림 수신:', alert);
            
            const alertWithId = {
              ...alert,
              id: Date.now() + Math.random(),
              timestamp: new Date(alert.timestamp)
            };
            
            setAlerts(prevAlerts => [
              alertWithId,
              ...prevAlerts.slice(0, 4) // 최대 5개만 유지
            ]);

            // 5초 후 알림 자동 제거
            setTimeout(() => {
              setAlerts(prevAlerts => 
                prevAlerts.filter(a => a.id !== alertWithId.id)
              );
            }, 5000);
          });

          // 회사별 실시간 데이터 구독
          const dataTopic = `/topic/data/${companyName}`;
          stompClient.current.subscribe(dataTopic, (message) => {
            const data = JSON.parse(message.body);
            console.log('실시간 데이터 수신:', data);
            
            setRealtimeData(data);
            
            // 부모 컴포넌트에 데이터 전달을 위한 커스텀 이벤트 발생
            window.dispatchEvent(new CustomEvent('realtimeDataUpdate', { 
              detail: data 
            }));
          });

          // 구독 메시지 전송
          stompClient.current.publish({
            destination: '/app/subscribe',
            body: JSON.stringify({
              companyName: companyName,
              userId: userId
            })
          });
        };

        // 에러 콜백 설정
        stompClient.current.onStompError = (frame) => {
          console.error('STOMP 에러:', frame);
          setIsConnected(false);
        };

        stompClient.current.onWebSocketError = (error) => {
          console.error('WebSocket 연결 실패:', error);
          setIsConnected(false);
        };

        // 연결 시작
        stompClient.current.activate();
      } catch (error) {
        console.error('웹소켓 초기화 오류:', error);
        setIsConnected(false);
      }
    };

    initWebSocket();

    // 정리 함수
    return () => {
      if (stompClient.current) {
        stompClient.current.deactivate();
        setIsConnected(false);
      }
    };
  }, [companyName, userId]);

  const removeAlert = (alertId) => {
    setAlerts(prevAlerts => prevAlerts.filter(alert => alert.id !== alertId));
  };

  return {
    isConnected,
    alerts,
    realtimeData,
    removeAlert
  };
};

export default useWebSocket;