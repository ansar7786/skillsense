import React from 'react';
import { Navigate, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function LandingPage() {
  const { isAuthenticated, user, loading, mustChangePassword } = useAuth();
  const navigate = useNavigate();

  // Preserve RootRedirect behavior: logged-in visitors are sent directly to their dashboard
  if (loading) {
    return (
      <div className="loading-container" role="status">
        <div className="spinner"></div>
        <p>Loading...</p>
      </div>
    );
  }

  if (isAuthenticated && user) {
    if (mustChangePassword) {
      return <Navigate to="/change-password" replace />;
    }
    if (user.role === 'STUDENT') {
      return <Navigate to="/student" replace />;
    }
    return <Navigate to="/officials" replace />;
  }

  return (
    <div className="landing-page">
      {/* Top Navigation Bar */}
      <nav className="landing-navbar" aria-label="Main Navigation">
        <div className="landing-nav-inner">
          <Link to="/" className="landing-brand">
            <span role="img" aria-label="chart logo" style={{ fontSize: '1.4rem' }}>📊</span>
            <span>SkillSense</span>
            <span className="landing-brand-tag">INSTITUTIONAL</span>
          </Link>

          <div className="d-flex align-center gap-3">
            <a href="#how-it-works" className="nav-link" style={{ fontSize: '0.92rem', color: 'var(--text-muted)' }}>
              How It Works
            </a>
            <a href="#features" className="nav-link" style={{ fontSize: '0.92rem', color: 'var(--text-muted)' }}>
              Features
            </a>
            <a href="#showcase" className="nav-link" style={{ fontSize: '0.92rem', color: 'var(--text-muted)' }}>
              Showcase
            </a>
            <Link
              to="/login"
              className="btn btn-secondary btn-sm"
              style={{ fontWeight: 600, padding: '6px 16px' }}
            >
              Sign In
            </Link>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <header className="landing-hero">
        <div className="landing-hero-bg-blobs" aria-hidden="true">
          <div className="hero-blob-1"></div>
          <div className="hero-blob-2"></div>
        </div>

        <div className="landing-hero-content">
          <span className="landing-section-tag">
            <span>🛡️</span> Privacy-First Higher Ed Analytics
          </span>
          <h1>SkillSense</h1>
          <p className="landing-hero-tagline">
            Unified survey, placement readiness, and student analytics platform for engineering institutions.
          </p>

          <div className="landing-hero-actions">
            <button
              type="button"
              className="btn btn-primary btn-hero-login"
              onClick={() => navigate('/login')}
            >
              Login
            </button>
            <a href="#showcase" className="btn-hero-secondary">
              See It in Action ↓
            </a>
          </div>
        </div>

        {/* Results that Matter — Mini-Stats Strip */}
        <div className="landing-stats-strip">
          <div className="landing-stats-grid">
            <div className="landing-kpi-card">
              <span className="kpi-label">Responses Collected</span>
              <div className="kpi-value tabular-nums">311+</div>
              <span className="kpi-meta text-muted">Across all surveyed cohorts</span>
            </div>

            <div className="landing-kpi-card">
              <span className="kpi-label">Avg Completion Rate</span>
              <div className="kpi-value tabular-nums">70%</div>
              <span className="kpi-meta text-muted">High voluntary student participation</span>
            </div>

            <div className="landing-kpi-card">
              <span className="kpi-label">Cohort Satisfaction Index</span>
              <div className="kpi-value tabular-nums">62.1</div>
              <span className="kpi-meta text-muted">0–100 normalized rating mean</span>
            </div>

            <div className="landing-kpi-card">
              <span className="kpi-label">Privacy Threshold</span>
              <div className="kpi-value tabular-nums">K = 5</div>
              <span className="kpi-meta text-muted">Automated group suppression gate</span>
            </div>
          </div>
        </div>
      </header>

      {/* What is SkillSense? */}
      <section className="landing-section landing-about" id="about">
        <div className="landing-section-header">
          <span className="landing-section-tag">Institutional Intelligence</span>
          <h2 className="landing-section-title">What is SkillSense?</h2>
          <p className="landing-section-subtitle">
            Bridging student voice with actionable faculty decisions and verifiable placement outcomes.
          </p>
        </div>

        <div className="landing-about-card">
          <p className="landing-about-text">
            SkillSense is an institutional intelligence platform designed to measure, analyze, and
            elevate engineering education. By bridging student feedback with actionable faculty and
            placement analytics, SkillSense provides deep visibility into academic curriculum satisfaction,
            campus facilities, and objective placement skill readiness.
          </p>
          <div className="landing-about-badges">
            <span className="category-pill category-placement">PLACEMENT BENCHMARKS</span>
            <span className="category-pill category-facility">FACILITY & HOSTEL AUDIT</span>
            <span className="category-pill category-academic">ACADEMIC DBMS FEEDBACK</span>
            <span className="category-pill category-wellbeing">WELLBEING PULSE</span>
            <span className="status-badge status-active">K=5 DIFFERENTIAL ANONYMITY</span>
          </div>
        </div>
      </section>

      {/* How It Works — Neumorphic Step Cards */}
      <section className="landing-section landing-how-it-works" id="how-it-works">
        <div className="landing-section-header">
          <span className="landing-section-tag">Methodology</span>
          <h2 className="landing-section-title">How It Works</h2>
          <p className="landing-section-subtitle">
            A deterministic, privacy-first pipeline from survey creation to executive AI recommendations.
          </p>
        </div>

        <ol className="how-it-works-grid">
          <li className="neumorphic-step-card">
            <div className="step-card-badge">01</div>
            <h3 className="step-card-title">Survey created:</h3>
            <p className="step-card-desc">
              Faculty and placement officials design targeted academic, placement readiness, and facility
              surveys with configurable competency scoring and audience filters.
            </p>
          </li>

          <li className="neumorphic-step-card">
            <div className="step-card-badge">02</div>
            <h3 className="step-card-title">Students respond anonymously:</h3>
            <p className="step-card-desc">
              Students submit feedback with strict cryptographic anonymity and K=5 group privacy guarantees.
              Submission participation tokens ensure exactly one submission without linking answers to identity.
            </p>
          </li>

          <li className="neumorphic-step-card">
            <div className="step-card-badge">03</div>
            <h3 className="step-card-title">Analytics engine computes readiness/skill-gaps:</h3>
            <p className="step-card-desc">
              The stateless analytics engine calculates objective benchmark readiness scores, satisfaction
              indices, and skill deficit breakdowns across technical and soft competencies.
            </p>
          </li>

          <li className="neumorphic-step-card">
            <div className="step-card-badge">04</div>
            <h3 className="step-card-title">AI insights + dashboards:</h3>
            <p className="step-card-desc">
              Institutional dashboards visualize trends across terms and Gemini AI delivers prioritized
              executive recommendations with deterministic rules engine fallback.
            </p>
          </li>
        </ol>
      </section>

      {/* Key Features — Floating Glassmorphic Grid */}
      <section className="landing-section landing-features" id="features">
        <div className="landing-section-header">
          <span className="landing-section-tag">Core Capabilities</span>
          <h2 className="landing-section-title">Key Features</h2>
          <p className="landing-section-subtitle">
            Enterprise-grade governance, mathematical privacy guarantees, and verifiable skill analytics.
          </p>
        </div>

        <ul className="features-glass-grid">
          <li className="glass-feature-card">
            <div className="feature-card-header">
              <div className="feature-icon-wrapper">🔒</div>
              <h3 className="feature-card-title">K=5 Anonymity and Differential Privacy Guarantees</h3>
            </div>
            <p className="feature-card-desc">
              Cohort slices with fewer than 5 respondents are automatically suppressed. Individual student
              identities never exist in survey response tables.
            </p>
          </li>

          <li className="glass-feature-card">
            <div className="feature-card-header">
              <div className="feature-icon-wrapper">🎯</div>
              <h3 className="feature-card-title">Placement Readiness &amp; Skill-Gap Scoring</h3>
            </div>
            <p className="feature-card-desc">
              Empirical readiness scoring mapped against institutional targets in core Java, Python, DSA,
              and Aptitude to pinpoint actionable curriculum gaps.
            </p>
          </li>

          <li className="glass-feature-card">
            <div className="feature-card-header">
              <div className="feature-icon-wrapper">👥</div>
              <h3 className="feature-card-title">Role-Based Access Control (RBAC)</h3>
            </div>
            <p className="feature-card-desc">
              Tailored workspaces for Students, Faculty, Department Heads (HOD), Placement Officers,
              and Institutional Administrators.
            </p>
          </li>

          <li className="glass-feature-card">
            <div className="feature-card-header">
              <div className="feature-icon-wrapper">📈</div>
              <h3 className="feature-card-title">Interactive Chart.js Visualizations</h3>
            </div>
            <p className="feature-card-desc">
              Real-time frequency distribution bars, satisfaction donuts, and academic year comparative
              breakdowns with clean indigo aesthetic.
            </p>
          </li>

          <li className="glass-feature-card">
            <div className="feature-card-header">
              <div className="feature-icon-wrapper">✨</div>
              <h3 className="feature-card-title">Gemini AI Executive Insights</h3>
            </div>
            <p className="feature-card-desc">
              Automated synthesis of quantitative data and student comments into prioritized action plans,
              backed by a zero-dependency deterministic rules fallback.
            </p>
          </li>

          <li className="glass-feature-card">
            <div className="feature-card-header">
              <div className="feature-icon-wrapper">📑</div>
              <h3 className="feature-card-title">Audit-Ready Exportable Reports</h3>
            </div>
            <p className="feature-card-desc">
              One-click PDF and Excel export generation with automated privacy gating and accreditation
              compliance formatting.
            </p>
          </li>
        </ul>
      </section>

      {/* See It in Action — Staggered Product Mockup Showcase */}
      <section className="landing-section landing-showcase" id="showcase">
        <div className="landing-section-header">
          <span className="landing-section-tag">Product Showcase</span>
          <h2 className="landing-section-title">See It in Action</h2>
          <p className="landing-section-subtitle">
            Real production screens from the live SkillSense platform deployed across engineering departments.
          </p>
        </div>

        <div className="showcase-staggered-grid">
          {/* Mockup 1: Overview Dashboard */}
          <div className="browser-mockup">
            <div className="browser-mockup-header">
              <div className="browser-mockup-dots" aria-hidden="true">
                <span className="browser-mockup-dot dot-red"></span>
                <span className="browser-mockup-dot dot-yellow"></span>
                <span className="browser-mockup-dot dot-green"></span>
              </div>
              <div className="browser-mockup-url">
                https://skillsense.edu/officials/surveys/analytics/overview
              </div>
            </div>
            <img
              src="/screenshots/analytics-overview.png"
              alt="Live survey analytics overview showing 90 responses, 70% completion, and 62.1 satisfaction index"
              className="browser-mockup-screen"
              loading="lazy"
            />
            <div className="browser-mockup-caption">
              <div className="caption-meta-row">
                <span className="category-pill category-facility">FACILITY AUDIT</span>
                <span className="status-badge status-active">ANONYMOUS (K=5)</span>
              </div>
              <h4 className="caption-title">Live Analytics with K=5 Anonymity Protection</h4>
              <p className="caption-desc">
                High-level completion rates, satisfaction indices, and privacy indicators across cohorts.
              </p>
            </div>
          </div>

          {/* Mockup 2: AI Insights (Staggered Column Offset) */}
          <div className="browser-mockup showcase-col-offset">
            <div className="browser-mockup-header">
              <div className="browser-mockup-dots" aria-hidden="true">
                <span className="browser-mockup-dot dot-red"></span>
                <span className="browser-mockup-dot dot-yellow"></span>
                <span className="browser-mockup-dot dot-green"></span>
              </div>
              <div className="browser-mockup-url">
                https://skillsense.edu/officials/surveys/analytics/insights
              </div>
            </div>
            <img
              src="/screenshots/ai-insights.jpg"
              alt="AI-generated executive insights with prioritized P1, P2, and P3 institutional recommendations"
              className="browser-mockup-screen"
              loading="lazy"
            />
            <div className="browser-mockup-caption">
              <div className="caption-meta-row">
                <span className="category-pill category-placement">GEMINI 2.5 FLASH</span>
                <span className="status-badge status-active">PRIORITIZED ACTION</span>
              </div>
              <h4 className="caption-title">AI-Generated Insights with Prioritized Recommendations</h4>
              <p className="caption-desc">
                Executive summaries and structured P1/P2/P3 action cards assigned to specific departments.
              </p>
            </div>
          </div>

          {/* Mockup 3: Admin Survey Dashboard */}
          <div className="browser-mockup">
            <div className="browser-mockup-header">
              <div className="browser-mockup-dots" aria-hidden="true">
                <span className="browser-mockup-dot dot-red"></span>
                <span className="browser-mockup-dot dot-yellow"></span>
                <span className="browser-mockup-dot dot-green"></span>
              </div>
              <div className="browser-mockup-url">
                https://skillsense.edu/officials/surveys
              </div>
            </div>
            <img
              src="/screenshots/survey-dashboard.png"
              alt="Institutional survey management dashboard with active survey metrics and response filters"
              className="browser-mockup-screen"
              loading="lazy"
            />
            <div className="browser-mockup-caption">
              <div className="caption-meta-row">
                <span className="category-pill category-academic">INSTITUTIONAL HUB</span>
                <span className="status-badge status-active">5 ACTIVE</span>
              </div>
              <h4 className="caption-title">Institutional Survey Management &amp; Cohort Targeting</h4>
              <p className="caption-desc">
                Centralized dashboard managing academic terms, response quotas, and multi-category surveys.
              </p>
            </div>
          </div>

          {/* Mockup 4: Student Survey Taking (Staggered Column Offset) */}
          <div className="browser-mockup showcase-col-offset">
            <div className="browser-mockup-header">
              <div className="browser-mockup-dots" aria-hidden="true">
                <span className="browser-mockup-dot dot-red"></span>
                <span className="browser-mockup-dot dot-yellow"></span>
                <span className="browser-mockup-dot dot-green"></span>
              </div>
              <div className="browser-mockup-url">
                https://skillsense.edu/student/surveys/take
              </div>
            </div>
            <img
              src="/screenshots/student-survey.png"
              alt="Mobile-responsive student survey-taking flow with rating scales and single-select options"
              className="browser-mockup-screen"
              loading="lazy"
            />
            <div className="browser-mockup-caption">
              <div className="caption-meta-row">
                <span className="category-pill category-placement">STUDENT EXPERIENCE</span>
                <span className="status-badge status-active">ANONYMOUS INPUT</span>
              </div>
              <h4 className="caption-title">Distraction-Free Student Response Interface</h4>
              <p className="caption-desc">
                Accessible rating scales, clear feedback inputs, and instant personal results upon submission.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Footer CTA */}
      <footer className="landing-footer">
        <div className="landing-footer-content">
          <span className="landing-section-tag">Get Started</span>
          <h3 className="landing-footer-title">
            Ready to measure and improve institutional outcomes?
          </h3>
          <p className="landing-footer-desc">
            Sign in with your institutional credentials to access active surveys, departmental analytics,
            and placement readiness benchmarks.
          </p>

          <button
            type="button"
            className="btn btn-primary btn-hero-login"
            onClick={() => navigate('/login')}
          >
            Login
          </button>

          <div className="landing-footer-meta">
            <p>&copy; {new Date().getFullYear()} SkillSense. All rights reserved.</p>
            <p style={{ marginTop: '4px', fontSize: '0.8rem' }}>
              Institutional Single-Sign-On &bull; Role-Based Governance &bull; K=5 Privacy Protection
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
}
