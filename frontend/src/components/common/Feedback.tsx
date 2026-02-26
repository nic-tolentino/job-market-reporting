import { useState, useRef, useEffect } from 'react';
import { MessageSquare, X, Send, AlertCircle } from 'lucide-react';

interface FeedbackModalProps {
    isOpen: boolean;
    onClose: () => void;
    context?: string;
}

export function FeedbackModal({ isOpen, onClose, context }: FeedbackModalProps) {
    const [status, setStatus] = useState<'idle' | 'submitting' | 'success'>('idle');
    const [feedback, setFeedback] = useState('');
    const modalRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (modalRef.current && !modalRef.current.contains(event.target as Node)) {
                onClose();
            }
        }
        if (isOpen) {
            document.addEventListener('mousedown', handleClickOutside);
        }
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [isOpen, onClose]);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        setStatus('submitting');
        // Simulated submission
        setTimeout(() => {
            setStatus('success');
            setTimeout(() => {
                onClose();
                setStatus('idle');
                setFeedback('');
            }, 2000);
        }, 800);
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-in fade-in duration-200">
            <div
                ref={modalRef}
                className="w-full max-w-md bg-white rounded-2xl shadow-2xl border border-gray-200 overflow-hidden transform animate-in slide-in-from-bottom-4 duration-300"
            >
                <div className="bg-slate-50 px-6 py-4 border-b border-gray-100 flex justify-between items-center">
                    <div className="flex items-center gap-2">
                        <div className="p-1.5 bg-blue-100 rounded-lg">
                            <MessageSquare className="h-4 w-4 text-blue-600" />
                        </div>
                        <h3 className="font-bold text-slate-900 text-lg">Send Feedback</h3>
                    </div>
                    <button onClick={onClose} className="p-2 hover:bg-gray-200 rounded-full transition-colors">
                        <X className="h-5 w-5 text-gray-500" />
                    </button>
                </div>

                <div className="p-6">
                    {status === 'success' ? (
                        <div className="py-8 text-center space-y-4">
                            <div className="inline-flex h-16 w-16 items-center justify-center rounded-full bg-green-100 text-green-600">
                                <Send className="h-8 w-8" />
                            </div>
                            <div>
                                <h4 className="text-xl font-bold text-slate-900">Thank you!</h4>
                                <p className="text-gray-500 mt-1">Your feedback helps us improve our market insights.</p>
                            </div>
                        </div>
                    ) : (
                        <form onSubmit={handleSubmit} className="space-y-4">
                            {context && (
                                <div className="flex items-start gap-2.5 p-3 rounded-xl bg-blue-50/50 border border-blue-100 mb-4">
                                    <AlertCircle className="h-4 w-4 text-blue-500 mt-0.5" />
                                    <div>
                                        <p className="text-xs font-bold text-blue-600 uppercase tracking-wider">Context</p>
                                        <p className="text-sm text-blue-900 font-medium">{context}</p>
                                    </div>
                                </div>
                            )}

                            <div>
                                <label className="block text-sm font-bold text-slate-700 mb-1.5">What can we improve?</label>
                                <textarea
                                    required
                                    value={feedback}
                                    onChange={(e) => setFeedback(e.target.value)}
                                    placeholder="Tell us about incorrect data, missing features, or bugs..."
                                    className="w-full h-32 px-4 py-3 rounded-xl border border-gray-200 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 transition-all outline-none text-sm resize-none placeholder:text-gray-400"
                                />
                            </div>

                            <button
                                type="submit"
                                disabled={status === 'submitting'}
                                className="w-full py-3 bg-slate-900 text-white font-bold rounded-xl hover:bg-slate-800 active:scale-[0.98] transition-all flex items-center justify-center gap-2 disabled:opacity-50"
                            >
                                {status === 'submitting' ? (
                                    <div className="h-5 w-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                                ) : (
                                    <>
                                        Submit Feedback
                                        <Send className="h-4 w-4" />
                                    </>
                                )}
                            </button>
                        </form>
                    )}
                </div>
            </div>
        </div>
    );
}

export function FeedbackButton({ context, variant = 'text' }: { context?: string, variant?: 'text' | 'icon' }) {
    const [isOpen, setIsOpen] = useState(false);

    if (variant === 'icon') {
        return (
            <>
                <button
                    onClick={() => setIsOpen(true)}
                    className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-all"
                    title="Provide feedback"
                >
                    <MessageSquare className="h-4 w-4" />
                </button>
                <FeedbackModal isOpen={isOpen} onClose={() => setIsOpen(false)} context={context} />
            </>
        );
    }

    return (
        <>
            <button
                onClick={() => setIsOpen(true)}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold text-slate-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-all border border-transparent hover:border-blue-100 group"
            >
                <MessageSquare className="h-3.5 w-3.5" />
                Provide feedback on this data
            </button>
            <FeedbackModal isOpen={isOpen} onClose={() => setIsOpen(false)} context={context} />
        </>
    );
}
