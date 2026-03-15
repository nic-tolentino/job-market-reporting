import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';
import { StatusBadge } from './StatusBadge';

describe('StatusBadge', () => {
  it('renders ACTIVE status correctly', () => {
    render(<StatusBadge status="ACTIVE" />);
    const badge = screen.getByText('Active');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-green-100');
    expect(badge).toHaveClass('text-green-800');
  });

  it('renders BLOCKED status correctly', () => {
    render(<StatusBadge status="BLOCKED" />);
    const badge = screen.getByText('Blocked');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-red-100');
  });

  it('renders NONE status for null or unknown', () => {
    const { rerender } = render(<StatusBadge status={null} />);
    expect(screen.getByText('No seed')).toBeInTheDocument();
    
    rerender(<StatusBadge status="UNKNOWN_XYZ" />);
    expect(screen.getByText('No seed')).toBeInTheDocument();
  });

  it('respects the size prop', () => {
    const { rerender } = render(<StatusBadge status="ACTIVE" size="sm" />);
    expect(screen.getByText('Active')).toHaveClass('text-xs');

    rerender(<StatusBadge status="ACTIVE" size="md" />);
    expect(screen.getByText('Active')).toHaveClass('text-sm');
  });
});
