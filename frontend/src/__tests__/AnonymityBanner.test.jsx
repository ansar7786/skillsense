import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import AnonymityBanner from '../components/AnonymityBanner';

describe('AnonymityBanner Component', () => {
  it('displays exact specification wording for anonymous surveys', () => {
    render(<AnonymityBanner isAnonymous={true} />);
    expect(
      screen.getByText('Anonymous: your name is not stored with your answers')
    ).toBeInTheDocument();
  });

  it('displays exact specification wording for non-anonymous surveys', () => {
    render(<AnonymityBanner isAnonymous={false} />);
    expect(
      screen.getByText('Not anonymous: your name is recorded with your answers')
    ).toBeInTheDocument();
  });
});
