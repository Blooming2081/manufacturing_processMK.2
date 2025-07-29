import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import apiService from "../service/apiService";

const Login = ({ onLogin }) => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      const loginResponse = await apiService.user.login({ username, password });
      
      if (loginResponse.success) {
        console.log('로그인 응답:', loginResponse);
        console.log('회사명:', loginResponse.companyName);
        
        localStorage.setItem('accessToken', loginResponse.accessToken);
        localStorage.setItem('refreshToken', loginResponse.refreshToken);
        
        const userData = {
          userName: loginResponse.userName,
          companyName: loginResponse.companyName,
          userId: loginResponse.userId,
          companyId: loginResponse.companyId
        };
        localStorage.setItem('userData', JSON.stringify(userData));
        localStorage.setItem('isLoggedIn', 'true');
        
        if (window.SetCompanyName && loginResponse.companyName) {
          setTimeout(() => {
            window.SetCompanyName(loginResponse.companyName);
          }, 1000);
        }
        
        const redirectUrl = `/${loginResponse.companyName}/dashboard`;
        console.log('리다이렉트 URL:', redirectUrl);
        
        onLogin(userData);
        navigate(redirectUrl);
      } else {
        setError("잘못된 사용자명 또는 비밀번호입니다.");
      }
    } catch (err) {
      console.error('로그인 오류:', err);
      setError("서버 연결에 실패했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="vh-100 d-flex justify-content-center align-items-center" 
         style={{ 
           background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
           position: 'relative',
           overflow: 'hidden'
         }}>
      
      {/* 배경 애니메이션 원들 */}
      <div style={{
        position: 'absolute',
        width: '300px',
        height: '300px',
        borderRadius: '50%',
        background: 'rgba(255, 255, 255, 0.1)',
        top: '-150px',
        right: '-150px',
        animation: 'float 6s ease-in-out infinite'
      }}></div>
      <div style={{
        position: 'absolute',
        width: '200px',
        height: '200px',
        borderRadius: '50%',
        background: 'rgba(255, 255, 255, 0.1)',
        bottom: '-100px',
        left: '-100px',
        animation: 'float 6s ease-in-out infinite 2s'
      }}></div>
      <div style={{
        position: 'absolute',
        width: '150px',
        height: '150px',
        borderRadius: '50%',
        background: 'rgba(255, 255, 255, 0.1)',
        top: '50%',
        right: '10%',
        animation: 'float 6s ease-in-out infinite 4s'
      }}></div>


      <div className="container">
        <div className="row align-items-center justify-content-between" style={{ minHeight: '100vh', padding: '0 5rem' }}>
          {/* 왼쪽 컨텐츠 */}
          <div className="col-lg-6 text-white">
            <h1 className="display-1 fw-bolder lh-1 mb-4" style={{ textShadow: '0 4px 20px rgba(0, 0, 0, 0.3)' }}>
              We Build<br/>
              Digital<br/>
              Factory
            </h1>
            <p className="fs-4 mb-5 opacity-75 fw-light">We Design | We Develop | We Inspire.</p>
            <a href="#learn-more" className="btn btn-outline-light btn-lg px-4 py-3 fw-semibold" 
               style={{ backdropFilter: 'blur(10px)', background: 'rgba(255, 255, 255, 0.2)' }}>
              LEARN MORE
            </a>
          </div>

          {/* 로그인 폼 */}
          <div className="col-lg-6 col-xl-5">
            <div className="card border-0 shadow-lg" 
                 style={{ 
                   background: 'rgba(255, 255, 255, 0.95)',
                   backdropFilter: 'blur(20px)',
                   borderRadius: '24px'
                 }}>
              <div className="card-body px-5 py-4">
                <h2 className="card-title text-center fw-bold fs-2 mb-2">Login</h2>
                <p className="text-center text-muted mb-4">안녕하세요. U1 MOBIS 입니다.</p>

                <form onSubmit={handleSubmit}>
                  <div className="mb-4">
                    <div className="input-group">
                      <span className="input-group-text border-0 bg-light">
                        <i className="ti ti-user fs-5"></i>
                      </span>
                      <input
                        type="text"
                        className="form-control form-control-lg border-0 bg-light"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        placeholder="아이디 또는 이메일을 입력하세요."
                        required
                        style={{ fontSize: '0.9rem' }}
                      />
                    </div>
                  </div>

                  <div className="mb-4">
                    <div className="input-group">
                      <span className="input-group-text border-0 bg-light">
                        <i className="ti ti-lock fs-5"></i>
                      </span>
                      <input
                        type="password"
                        className="form-control form-control-lg border-0 bg-light"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="비밀번호를 입력하세요."
                        required
                        style={{ fontSize: '0.9rem' }}
                      />
                    </div>
                  </div>

                  {error && (
                    <div className="alert alert-danger py-2 mb-4">{error}</div>
                  )}

                  <button 
                    type="submit" 
                    className="btn btn-lg w-100 text-white fw-semibold mb-4"
                    style={{ 
                      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                      border: 'none'
                    }}
                    disabled={loading}
                  >
                    {loading ? "로그인 중..." : "로그인"}
                  </button>
                </form>

                <div className="text-center mb-4">
                  <div className="text-muted mb-3">
                    <span>계정이 없으신가요? </span>
                    <a
                      href="#"
                      className="text-decoration-none fw-semibold"
                      style={{ color: '#667eea' }}
                      onClick={(e) => {
                        e.preventDefault();
                        navigate("/register"); 
                      }}
                    >
                      회원가입
                    </a>
                  </div>
                  <div className="text-muted">
                    <span>새로운 회사인가요? </span>
                    <a
                      href="#"
                      className="text-decoration-none fw-semibold"
                      style={{ color: '#667eea' }}
                      onClick={(e) => {
                        e.preventDefault();
                        navigate("/company-register"); 
                      }}
                    >
                      회사등록
                    </a>
                  </div>
                </div>

              </div>
            </div>
          </div>
        </div>
      </div>

      <style jsx="true">{`
        @keyframes float {
          0%, 100% { 
            transform: translateY(0px) rotate(0deg); 
            opacity: 0.7; 
          }
          50% { 
            transform: translateY(-20px) rotate(180deg); 
            opacity: 0.3; 
          }
        }
        
        .form-control:focus {
          border-color: #667eea !important;
          box-shadow: 0 0 0 0.2rem rgba(102, 126, 234, 0.25) !important;
        }
        
        .btn:hover {
          transform: translateY(-2px);
          box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
        }
        
        .nav-link:hover {
          color: white !important;
          text-shadow: 0 0 10px rgba(255, 255, 255, 0.5);
        }
        
        @media (max-width: 1024px) {
          .row {
            flex-direction: column;
            text-align: center;
            gap: 2rem;
          }
        }
      `}</style>
    </div>
  );
};

export default Login;   
