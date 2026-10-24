import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import QuestionRenderer from '../components/QuestionRenderer';

describe('QuestionRenderer Component', () => {
  it('renders and interacts with SINGLE_CHOICE radio buttons', () => {
    const handleChange = vi.fn();
    const question = {
      key: 'single_q',
      type: 'SINGLE_CHOICE',
      prompt: 'Select your preferred role',
      required: true,
      options: [
        { value: 'swe', label: 'Software Engineer' },
        { value: 'data', label: 'Data Analyst' },
      ],
    };

    render(<QuestionRenderer question={question} value="" onChange={handleChange} />);

    expect(screen.getByText('Select your preferred role')).toBeInTheDocument();
    const sweRadio = screen.getByLabelText('Software Engineer');
    const dataRadio = screen.getByLabelText('Data Analyst');

    expect(sweRadio).not.toBeChecked();
    expect(dataRadio).not.toBeChecked();

    fireEvent.click(sweRadio);
    expect(handleChange).toHaveBeenCalledWith('single_q', 'swe');
  });

  it('renders and interacts with MULTI_CHOICE checkboxes', () => {
    const handleChange = vi.fn();
    const question = {
      key: 'multi_q',
      type: 'MULTI_CHOICE',
      prompt: 'Select programming languages',
      required: false,
      options: [
        { value: 'python', label: 'Python' },
        { value: 'java', label: 'Java' },
        { value: 'cpp', label: 'C++' },
      ],
    };

    render(<QuestionRenderer question={question} value={['python']} onChange={handleChange} />);

    const pythonBox = screen.getByLabelText('Python');
    const javaBox = screen.getByLabelText('Java');

    expect(pythonBox).toBeChecked();
    expect(javaBox).not.toBeChecked();

    fireEvent.click(javaBox);
    expect(handleChange).toHaveBeenCalledWith('multi_q', ['python', 'java']);

    fireEvent.click(pythonBox);
    expect(handleChange).toHaveBeenCalledWith('multi_q', []);
  });

  it('renders and interacts with RATING buttons', () => {
    const handleChange = vi.fn();
    const question = {
      key: 'rating_q',
      type: 'RATING',
      prompt: 'Rate your confidence',
      required: true,
      rating: {
        min: 1,
        max: 5,
        minLabel: 'Beginner',
        maxLabel: 'Expert',
      },
    };

    render(<QuestionRenderer question={question} value={3} onChange={handleChange} />);

    expect(screen.getByText('Beginner')).toBeInTheDocument();
    expect(screen.getByText('Expert')).toBeInTheDocument();

    const btn4 = screen.getByRole('radio', { name: '4' });
    fireEvent.click(btn4);
    expect(handleChange).toHaveBeenCalledWith('rating_q', 4);
  });

  it('renders and interacts with YES_NO buttons', () => {
    const handleChange = vi.fn();
    const question = {
      key: 'yes_no_q',
      type: 'YES_NO',
      prompt: 'Do you understand the syllabus?',
      required: true,
    };

    render(<QuestionRenderer question={question} value={null} onChange={handleChange} />);

    const yesBtn = screen.getByRole('radio', { name: 'Yes' });
    const noBtn = screen.getByRole('radio', { name: 'No' });

    fireEvent.click(yesBtn);
    expect(handleChange).toHaveBeenCalledWith('yes_no_q', true);

    fireEvent.click(noBtn);
    expect(handleChange).toHaveBeenCalledWith('yes_no_q', false);
  });

  it('renders and interacts with NUMBER input respecting min and max', () => {
    const handleChange = vi.fn();
    const question = {
      key: 'num_q',
      type: 'NUMBER',
      prompt: 'How many coding problems solved?',
      required: true,
      min: 0,
      max: 1000,
    };

    render(<QuestionRenderer question={question} value={50} onChange={handleChange} />);

    const input = screen.getByLabelText(/How many coding problems solved/);
    expect(input).toHaveValue(50);
    expect(input).toHaveAttribute('min', '0');
    expect(input).toHaveAttribute('max', '1000');

    fireEvent.change(input, { target: { value: '75' } });
    expect(handleChange).toHaveBeenCalledWith('num_q', 75);

    fireEvent.change(input, { target: { value: '' } });
    expect(handleChange).toHaveBeenCalledWith('num_q', null);
  });

  it('renders and interacts with TEXT textarea respecting maxLength', () => {
    const handleChange = vi.fn();
    const question = {
      key: 'text_q',
      type: 'TEXT',
      prompt: 'Provide feedback',
      required: false,
      maxLength: 200,
    };

    render(<QuestionRenderer question={question} value="Initial note" onChange={handleChange} />);

    const textarea = screen.getByLabelText(/Provide feedback/);
    expect(textarea).toHaveValue('Initial note');
    expect(textarea).toHaveAttribute('maxLength', '200');
    expect(screen.getByText('12 / 200')).toBeInTheDocument();

    fireEvent.change(textarea, { target: { value: 'Updated feedback note' } });
    expect(handleChange).toHaveBeenCalledWith('text_q', 'Updated feedback note');
  });

  it('displays validation error when error prop is provided', () => {
    const question = {
      key: 'req_q',
      type: 'TEXT',
      prompt: 'Required question',
      required: true,
    };

    render(
      <QuestionRenderer
        question={question}
        value=""
        onChange={() => {}}
        error="This question is required."
      />
    );

    expect(screen.getByRole('alert')).toHaveTextContent('This question is required.');
  });
});
