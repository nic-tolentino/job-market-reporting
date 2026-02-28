import { Card } from '../components/ui/Card';
import { H2 } from '../components/ui/Typography';

export default function PrivacyPolicyPage() {
    return (
        <div className="max-w-4xl mx-auto space-y-10 py-8 md:py-12">
            <section className="text-center px-4">
                <h1 className="text-3xl md:text-5xl font-extrabold tracking-tight text-slate-900 leading-tight">
                    Privacy Policy
                </h1>
                <p className="mx-auto mt-4 max-w-2xl text-lg text-gray-600">
                    Effective Date: February 28, 2026
                </p>
            </section>

            <Card className="p-0 overflow-hidden">
                <div className="p-6 md:p-8 prose prose-slate max-w-none text-gray-600 space-y-6">
                    <p>
                        This Privacy Policy is designed to be clear, transparent, and compliant with both the <strong>New Zealand Privacy Act 2020</strong> and the <strong>Australian Privacy Act 1988</strong>.
                    </p>

                    <div>
                        <H2 className="text-slate-900 mb-2">1. Introduction</H2>
                        <p>
                            This Privacy Policy explains how TechMarket ("we", "our", or "the Project") collects, uses, and manages data. This is an open-source, non-profit personal project developed by <strong>Nic Tolentino</strong> to support the engineering communities in Australia and New Zealand. Our mission is to provide market transparency and reinvest any surplus revenue into community initiatives such as developer scholarships and GDG events.
                        </p>
                    </div>

                    <div>
                        <H2 className="text-slate-900 mb-2">2. Information We Collect</H2>
                        <p>We handle two types of information:</p>
                        <ul className="list-disc pl-5 mt-2 space-y-2">
                            <li><strong>Aggregated Job Data (Scraped):</strong> We collect publicly available job listings from third-party platforms (e.g., LinkedIn, Seek). Our processing pipeline sanitizes this data immediately. We keep job titles, company names, tech stacks, salary bands, seniority levels, and locations. We explicitly <strong>discard</strong> personal identifiers such as recruiter names, emails, or phone numbers.</li>
                            <li><strong>User Data (Direct):</strong> We collect minimal log data (IP address, browser type) and any contact details you provide if you message us directly.</li>
                        </ul>
                    </div>

                    <div>
                        <H2 className="text-slate-900 mb-2">3. How We Use Your Information</H2>
                        <p>We use the collected data strictly for providing market insights and maintaining the website. We do <strong>not</strong> sell personal data to third parties.</p>
                    </div>

                    <div>
                        <H2 className="text-slate-900 mb-2">4. Your Rights</H2>
                        <p>
                            Under NZ and AU laws, you have the right to access, correction, and erasure of any personal data we hold. If you are a recruiter or job poster and wish for a specific listing to be removed or modified, please contact us.
                        </p>
                    </div>

                    <div className="pt-6 border-t border-gray-100">
                        <p className="font-semibold text-slate-900">Contact Us</p>
                        <p>Nic Tolentino</p>
                        <p>Auckland, New Zealand</p>
                        <p className="text-blue-600">hello@nictolentino.com</p>
                    </div>
                </div>
            </Card>
        </div>
    );
}
