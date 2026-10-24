import React from 'react';

export default function AnonymityBanner({ isAnonymous }) {
  const anonymousText = 'Anonymous: your name is not stored with your answers';
  const nonAnonymousText = 'Not anonymous: your name is recorded with your answers';

  return (
    <div
      className={`anonymity-banner ${isAnonymous ? 'banner-anonymous' : 'banner-named'}`}
      role="note"
      aria-label="Anonymity Notice"
    >
      <span className="banner-icon" aria-hidden="true">
        {isAnonymous ? '🔒' : '👤'}
      </span>
      <span className="banner-text">
        {isAnonymous ? anonymousText : nonAnonymousText}
      </span>
    </div>
  );
}
