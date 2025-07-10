import React, { useRef, useEffect, useState } from 'react';

const Factory3DTwin = () => {
  const unityContainerRef = useRef(null);
  const [isUnityLoaded, setIsUnityLoaded] = useState(false);
  const [loadingProgress, setLoadingProgress] = useState(0);
  const [errorMessage, setErrorMessage] = useState('');
  const unityInstanceRef = useRef(null);
  const isLoadingRef = useRef(false);
  const progressIntervalRef = useRef(null);

  useEffect(() => {
    const loadUnity = async () => {
      // 이미 로딩 중이거나 로드된 경우 중복 방지
      if (isLoadingRef.current || unityInstanceRef.current) {
        console.log('Unity 이미 로드됨 또는 로딩 중 - 스킵');
        return;
      }

      try {
        console.log('Unity 로드 시작...');
        isLoadingRef.current = true;
        setErrorMessage('');

        // 기존 Unity 캔버스 정리
        const existingCanvas = document.getElementById('unity-canvas');
        if (existingCanvas && existingCanvas !== unityContainerRef.current) {
          console.log('기존 Unity 캔버스 제거');
          existingCanvas.remove();
        }

        // 1. 로딩 진행률 시뮬레이션
        progressIntervalRef.current = setInterval(() => {
          setLoadingProgress(prev => {
            if (prev >= 98) return prev;
            return prev + Math.random() * 2;
          });
        }, 200);

        // 2. Unity 설정
        const buildUrl = '/unity3d';
        const config = {
          dataUrl: `${buildUrl}/factoryTwin.data`,
          frameworkUrl: `${buildUrl}/factoryTwin.framework.js`,
          codeUrl: `${buildUrl}/factoryTwin.wasm`
        };

        console.log('Unity 설정:', config);

        // 3. Unity 로더 방식 확인
        const loadUnityInstance = () => {
          // 컴포넌트가 언마운트되었는지 확인
          if (!isLoadingRef.current) {
            console.log('컴포넌트 언마운트됨 - Unity 로딩 중단');
            return;
          }

          // 이미 인스턴스가 있다면 스킵
          if (unityInstanceRef.current) {
            console.log('Unity 인스턴스 이미 존재 - 스킵');
            setIsUnityLoaded(true);
            setLoadingProgress(100);
            if (progressIntervalRef.current) {
              clearInterval(progressIntervalRef.current);
            }
            isLoadingRef.current = false;
            return;
          }

          const canvas = document.getElementById('unity-canvas');
          if (!canvas) {
            console.error('Unity 캔버스를 찾을 수 없습니다');
            setErrorMessage('Unity 캔버스 요소를 찾을 수 없습니다');
            if (progressIntervalRef.current) {
              clearInterval(progressIntervalRef.current);
            }
            isLoadingRef.current = false;
            return;
          }

          // Unity 6.1 방식 시도
          if (typeof window.createUnityInstance !== 'undefined') {
            console.log('createUnityInstance 사용');
            
            window.createUnityInstance(canvas, config, (progress) => {
              const progressPercent = progress * 100;
              setLoadingProgress(progressPercent);
            }).then((unityInstance) => {
              // 로딩 중에 컴포넌트가 언마운트되었는지 확인
              if (!isLoadingRef.current) {
                console.log('컴포넌트 언마운트됨 - Unity 인스턴스 정리');
                if (unityInstance && typeof unityInstance.Quit === 'function') {
                  try {
                    unityInstance.Quit();
                  } catch (e) {
                    console.log('Unity 정리 중 오류 (무시됨):', e.message);
                  }
                }
                return;
              }

              console.log('Unity 로드 성공!', unityInstance);
              unityInstanceRef.current = unityInstance;
              setIsUnityLoaded(true);
              setLoadingProgress(100);
              if (progressIntervalRef.current) {
                clearInterval(progressIntervalRef.current);
              }
              isLoadingRef.current = false;
              
              setupUnityReactCommunication(unityInstance);
            }).catch((error) => {
              console.error('Unity 로드 실패:', error);
              setErrorMessage(`Unity 로드 실패: ${error.message || error.toString()}`);
              if (progressIntervalRef.current) {
                clearInterval(progressIntervalRef.current);
              }
              isLoadingRef.current = false;
            });
          } else {
            // Unity 로더 대기
            console.log('Unity 로더 대기 중...');
            setTimeout(loadUnityInstance, 1000);
          }
        };

        // 4. Framework 스크립트 로드
        const loadFramework = () => {
          // 이미 로드되어 있는지 확인
          if (window.createUnityInstance) {
            loadUnityInstance();
            return;
          }

          // Framework 스크립트 동적 로드
          const script = document.createElement('script');
          script.src = `${buildUrl}/factoryTwin.framework.js`;
          script.async = true;
          
          script.onload = () => {
            console.log('Unity Framework 로드 완료');
            setTimeout(loadUnityInstance, 500);
          };
          
          script.onerror = (error) => {
            console.error('Framework 로드 실패:', error);
            setErrorMessage('Unity Framework 로드 실패 - factoryTwin.framework.js를 확인하세요');
            if (progressIntervalRef.current) {
              clearInterval(progressIntervalRef.current);
            }
            isLoadingRef.current = false;
          };

          document.head.appendChild(script);
        };

        // 5. Loader 스크립트 로드 (있다면)
        const loaderScript = document.createElement('script');
        loaderScript.src = `${buildUrl}/factoryTwin.loader.js`;
        loaderScript.async = true;
        
        loaderScript.onload = () => {
          console.log('Unity Loader 로드 완료');
          loadFramework();
        };
        
        loaderScript.onerror = () => {
          console.log('Loader 없음 - Framework 직접 로드');
          loadFramework();
        };

        document.head.appendChild(loaderScript);

      } catch (error) {
        console.error('Unity 초기화 오류:', error);
        setErrorMessage(`Unity 초기화 실패: ${error.message}`);
        if (progressIntervalRef.current) {
          clearInterval(progressIntervalRef.current);
        }
        isLoadingRef.current = false;
      }
    };

    // Unity 로드 시작
    const timer = setTimeout(loadUnity, 100);

    return () => {
      console.log('Factory3DTwin 컴포넌트 언마운트 시작');
      clearTimeout(timer);
      isLoadingRef.current = false;

      // 진행률 타이머 정리
      if (progressIntervalRef.current) {
        clearInterval(progressIntervalRef.current);
        progressIntervalRef.current = null;
      }

      // Unity 인스턴스 정리 (안전하게)
      if (unityInstanceRef.current) {
        const unityInstance = unityInstanceRef.current;
        unityInstanceRef.current = null;
        
        // 약간의 지연을 두고 정리
        setTimeout(() => {
          try {
            console.log('Unity 인스턴스 정리 시작');
            if (typeof unityInstance.Quit === 'function') {
              unityInstance.Quit();
              console.log('Unity 인스턴스 정리 완료');
            }
          } catch (e) {
            console.log('Unity 정리 중 예상된 오류 (무시됨):', e.message);
          }
        }, 100);
      }

      // 동적으로 추가된 스크립트 제거 (안전하게)
      try {
        const scripts = document.querySelectorAll('script[src*="factoryTwin"]');
        scripts.forEach(script => {
          if (script.parentNode) {
            script.parentNode.removeChild(script);
          }
        });
      } catch (e) {
        console.log('스크립트 정리 중 오류 (무시됨):', e.message);
      }
    };
  }, []);

  // Unity와 React 간 통신 설정
  const setupUnityReactCommunication = (unityInstance) => {
    console.log('Unity-React 통신 설정');
    
    // React에서 Unity로 데이터 전송
    window.SendToUnity = (gameObjectName, methodName, parameter) => {
      try {
        if (unityInstance && unityInstance.SendMessage) {
          unityInstance.SendMessage(gameObjectName, methodName, parameter);
          console.log('Unity로 메시지 전송:', { gameObjectName, methodName, parameter });
        }
      } catch (error) {
        console.error('Unity 메시지 전송 실패:', error);
      }
    };

    // Unity에서 React로 데이터 수신
    window.ReceiveFromUnity = (data) => {
      try {
        const parsedData = typeof data === 'string' ? JSON.parse(data) : data;
        console.log('Unity에서 받은 데이터:', parsedData);
        handleUnityData(parsedData);
      } catch (error) {
        console.error('Unity 데이터 파싱 오류:', error);
      }
    };
  };

  // Unity 데이터 처리
  const handleUnityData = (data) => {
    switch (data.type) {
      case 'robotClicked':
        console.log('로봇 클릭됨:', data.payload);
        // TODO: 로봇 정보 모달 표시
        break;
      case 'processClicked':
        console.log('공정 클릭됨:', data.payload);
        // TODO: 공정 정보 표시
        break;
      case 'statusUpdate':
        console.log('상태 업데이트:', data.payload);
        // TODO: 실시간 상태 업데이트
        break;
      default:
        console.log('기타 Unity 데이터:', data);
    }
  };

  const handleRetry = () => {
    console.log('Unity 재시도 시작');
    
    // 기존 상태 리셋
    setErrorMessage('');
    setLoadingProgress(0);
    setIsUnityLoaded(false);
    isLoadingRef.current = false;
    
    // 진행률 타이머 정리
    if (progressIntervalRef.current) {
      clearInterval(progressIntervalRef.current);
      progressIntervalRef.current = null;
    }
    
    // 기존 Unity 인스턴스 정리
    if (unityInstanceRef.current) {
      try {
        if (typeof unityInstanceRef.current.Quit === 'function') {
          unityInstanceRef.current.Quit();
        }
      } catch (e) {
        console.log('재시도 중 Unity 정리 오류 (무시됨):', e.message);
      }
      unityInstanceRef.current = null;
    }
    
    // 기존 스크립트 제거
    try {
      const scripts = document.querySelectorAll('script[src*="factoryTwin"]');
      scripts.forEach(script => {
        if (script.parentNode) {
          script.parentNode.removeChild(script);
        }
      });
    } catch (e) {
      console.log('스크립트 제거 중 오류 (무시됨):', e.message);
    }
    
    // 페이지 새로고침
    setTimeout(() => {
      window.location.reload();
    }, 500);
  };

  const handleSkipUnity = () => {
    console.log('Unity 건너뛰기');
    setErrorMessage('');
    setIsUnityLoaded(true);
    setLoadingProgress(100);
    isLoadingRef.current = false;
    
    // 진행률 타이머 정리
    if (progressIntervalRef.current) {
      clearInterval(progressIntervalRef.current);
      progressIntervalRef.current = null;
    }
  };

  return (
    <div style={{ width: '100%', height: '100%', position: 'relative' }}>
      {/* Unity 캔버스 */}
      <canvas
        id="unity-canvas"
        ref={unityContainerRef}
        style={{
          width: '100%',
          height: '100%',
          display: isUnityLoaded ? 'block' : 'none',
          backgroundColor: '#2c3e50'
        }}
      />
      
      {/* 로딩 화면 */}
      {!isUnityLoaded && !errorMessage && (
        <div
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            width: '100%',
            height: '100%',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'center',
            alignItems: 'center',
            backgroundColor: '#f8f9fa'
          }}
        >
          <div style={{ marginBottom: '20px', fontSize: '18px', color: '#6c757d' }}>
            3D 팩토리 로딩 중...
          </div>
          
          <div
            style={{
              width: '300px',
              height: '20px',
              backgroundColor: '#e9ecef',
              borderRadius: '10px',
              overflow: 'hidden',
              marginBottom: '10px'
            }}
          >
            <div
              style={{
                width: `${loadingProgress}%`,
                height: '100%',
                backgroundColor: loadingProgress > 90 ? '#ffc107' : '#007bff',
                transition: 'width 0.3s ease'
              }}
            />
          </div>
          
          <div style={{ marginBottom: '15px', color: '#6c757d' }}>
            {Math.round(loadingProgress)}%
          </div>
        </div>
      )}

      {/* 에러 화면 */}
      {errorMessage && (
        <div
          style={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            textAlign: 'center',
            padding: '20px',
            backgroundColor: 'white',
            borderRadius: '8px',
            boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
            maxWidth: '500px'
          }}
        >
          <div style={{ marginBottom: '10px', fontSize: '16px', fontWeight: 'bold', color: '#dc3545' }}>
            Unity 3D 로드 실패
          </div>
          <div style={{ marginBottom: '15px', fontSize: '14px', color: '#6c757d' }}>
            {errorMessage}
          </div>
          <div style={{ marginBottom: '15px', fontSize: '12px', color: '#6c757d', textAlign: 'left' }}>
            <strong>확인사항:</strong><br/>
            1. 파일 경로: /public/unity3d/factoryTwin.*<br/>
            2. 브라우저 개발자 도구 → Network 탭에서 404 에러 확인<br/>
            3. Unity 빌드 설정에서 압축 해제<br/>
            4. 파일 권한 확인
          </div>
          <button
            onClick={handleRetry}
            style={{
              padding: '8px 16px',
              backgroundColor: '#007bff',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer'
            }}
          >
            다시 시도
          </button>
        </div>
      )}

      {/* Unity 로드 성공 후 대체 컨텐츠 */}
      {isUnityLoaded && !unityInstanceRef.current && (
        <div
          style={{
            width: '100%',
            height: '100%',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            backgroundColor: '#2c3e50',
            color: 'white',
            fontSize: '18px'
          }}
        >
          Unity 3D 뷰어 (개발 중)
          <br />
          <small style={{ marginTop: '10px', display: 'block', opacity: 0.7 }}>
            Unity 씬이 여기에 표시됩니다
          </small>
        </div>
      )}
    </div>
  );
};

export default Factory3DTwin;