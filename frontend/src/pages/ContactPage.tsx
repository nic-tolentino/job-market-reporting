import { Card } from '../components/ui/Card';
import { Mail, MapPin, Github } from 'lucide-react';

export default function ContactPage() {
    return (
        <div className="max-w-4xl mx-auto space-y-10 py-8 md:py-12">
            <section className="text-center px-4">
                <h1 className="text-3xl md:text-5xl font-extrabold tracking-tight text-primary leading-tight">
                    Get in Touch
                </h1>
                <p className="mx-auto mt-4 max-w-2xl text-lg text-secondary">
                    Questions, feedback, or just want to say hi? We'd love to hear from you.
                </p>
            </section>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-8 px-4">
                <Card className="flex flex-col items-center justify-center p-8 text-center space-y-4 hover:border-accent/30 transition-all">
                    <div className="h-12 w-12 rounded-full bg-accent-subtle flex items-center justify-center text-accent">
                        <Mail className="h-6 w-6" />
                    </div>
                    <h3 className="font-bold text-primary text-xl">Email Us</h3>
                    <p className="text-muted">For general inquiries or data removal requests.</p>
                    <a href="mailto:support@devassembly.org" className="text-accent font-semibold hover:underline">
                        support@devassembly.org
                    </a>
                </Card>

                <Card className="flex flex-col items-center justify-center p-8 text-center space-y-4 hover:border-emerald-200 dark:hover:border-emerald-500/30 transition-all">
                    <div className="h-12 w-12 rounded-full bg-emerald-50 dark:bg-emerald-500/10 flex items-center justify-center text-emerald-600 dark:text-emerald-400">
                        <MapPin className="h-6 w-6" />
                    </div>
                    <h3 className="font-bold text-primary text-xl">Location</h3>
                    <p className="text-muted">Based in the beautiful city of sails.</p>
                    <span className="text-emerald-700 dark:text-emerald-400 font-semibold">
                        Auckland, New Zealand
                    </span>
                </Card>
            </div>

            <Card className="mx-4 p-8 bg-slate-900 dark:bg-slate-800 text-white border-0 shadow-xl overflow-hidden relative">
                <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-6">
                    <div className="text-center md:text-left">
                        <h2 className="text-2xl font-bold">Open Source & Community</h2>
                        <p className="text-slate-400 mt-2">This project is built for the community. Contributions are welcome!</p>
                    </div>
                    <a
                        href="https://github.com/nic-tolentino/job-market-reporting"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center gap-2 bg-white text-slate-900 px-6 py-3 rounded-full font-bold hover:bg-slate-100 transition-colors"
                    >
                        <Github className="h-5 w-5" />
                        View Source
                    </a>
                </div>
                {/* Decorative background element */}
                <div className="absolute top-0 right-0 -mr-16 -mt-16 h-64 w-64 bg-blue-500/10 rounded-full blur-3xl"></div>
            </Card>
        </div>
    );
}
