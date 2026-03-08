import { Link } from 'react-router-dom';

export default function Footer() {
    return (
        <footer className="mt-auto border-t border-border bg-card py-12">
            <div className="mx-auto max-w-6xl px-4 sm:px-6 lg:px-8">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-8">
                    <div className="max-w-md">
                        <p className="text-sm font-bold text-primary mb-4 uppercase tracking-wider">Our Mission</p>
                        <p className="text-sm text-muted leading-relaxed">
                            Built with ❤️ by <strong>Nic Tolentino</strong>. This is an open-source, non-profit project dedicated to helping the NZ and AU engineering communities. All proceeds are reinvested into local developer scholarships and community events.
                        </p>
                    </div>
                    <div className="flex flex-col md:items-end gap-4 text-sm font-semibold text-secondary">
                        <p className="text-sm font-bold text-primary mb-0 uppercase tracking-wider">Quick Links</p>
                        <nav className="flex flex-wrap gap-x-6 gap-y-2 md:justify-end">
                            <Link to="/transparency" className="hover:text-accent transition-colors">Transparency</Link>
                            <Link to="/pipeline" className="hover:text-accent transition-colors">Pipeline</Link>
                            <Link to="/privacy" className="hover:text-accent transition-colors">Privacy Policy</Link>
                            <Link to="/terms" className="hover:text-accent transition-colors">Terms of Service</Link>
                            <Link to="/contact" className="hover:text-accent transition-colors">Contact</Link>
                        </nav>
                    </div>
                </div>
                <div className="pt-8 border-t border-border-subtle flex flex-col items-center justify-between gap-4 sm:flex-row">
                    <p className="text-sm text-muted">
                        &copy; {new Date().getFullYear()} DevAssembly. Data extracted for public interest.
                    </p>
                    <div className="flex gap-4">
                        <a href="https://github.com/nic-tolentino/job-market-reporting" target="_blank" rel="noopener noreferrer" className="text-muted hover:text-primary transition-colors">
                            GitHub
                        </a>
                    </div>
                </div>
            </div>
        </footer>
    );
}
