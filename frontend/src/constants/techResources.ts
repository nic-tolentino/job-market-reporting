export interface Resource {
    title: string;
    url: string;
    description: string;
    previewImage?: string; // Optional thumbnail/preview image
    imageUrl?: string;     // Direct image link for the resource
    location?: string;     // e.g. "Global", "Auckland", "Wellington", "Alexandra"
}

export interface TechResources {
    youtube: Resource[];
    courses: Resource[];
    podcasts: Resource[];
    websites: Resource[];
    projects: Resource[];
    people: Resource[];
}

export interface CommunityResources {
    meetups: Resource[];
    events: Resource[];
    localProjects: Resource[];
    celebrities: Resource[];
}

export const LEARN_RESOURCES: Record<string, TechResources> = {
    android: {
        youtube: [
            { title: "Android Developers", url: "https://www.youtube.com/@AndroidDevelopers", description: "Official Android developer channel (Global Insights)", previewImage: "https://www.gstatic.com/android/devsite/56d0d5d5a9d4e5d5/android/images/lockup_android_developers.png", location: "Global" },
            { title: "Philipp Lackner", url: "https://www.youtube.com/@PhilippLackner", description: "Jetpack Compose & Architecture expert.", previewImage: "/assets/previews.png", location: "Global" }
        ],
        courses: [
            { title: "Android Basics with Compose", url: "https://developer.android.com/courses/android-basics-compose/course", description: "The definitive guide for modern Android", previewImage: "https://developer.android.com/static/images/courses/android-basics-compose/banner.png", location: "Global" },
            { title: "Developing Android Apps with Kotlin", url: "https://www.udacity.com/course/developing-android-apps-with-kotlin--ud9012", description: "Deep dive into production-ready Kotlin apps", previewImage: "/assets/previews.png", location: "Global" }
        ],
        podcasts: [
            { title: "Android Faithful", url: "https://androidfaithful.com/", description: "Cutting-edge Android news and banter", previewImage: "/assets/previews.png", location: "Global" },
            { title: "Fragmented", url: "https://fragmentedpodcast.com/", description: "Architecture and software engineering for Android", location: "Global" }
        ],
        websites: [
            { title: "Android Weekly", url: "https://androidweekly.net/", description: "Curated newsletter followed by millions", previewImage: "/assets/previews.png", location: "Global" },
            { title: "Kotlin Documentation", url: "https://kotlinlang.org/docs/home.html", description: "The source of truth for the language", previewImage: "https://kotlinlang.org/assets/images/twitter-card.png", location: "Global" }
        ],
        projects: [
            { title: "Now in Android", url: "https://github.com/android/nowinandroid", description: "Best-practice sample for modern architectures", previewImage: "https://github.com/android/nowinandroid/raw/main/tools/screenshots/main.png", location: "Global" }
        ],
        people: [
            { title: "Jake Wharton", url: "https://jakewharton.com/", description: "Android legend, creator of Timber and Butterknife.", previewImage: "https://avatars.githubusercontent.com/u/66577?v=4", location: "Global" },
            { title: "Mark Murphy", url: "https://commonsware.com/", description: "Founder of CommonsWare and author of The Busy Coder's Guide.", previewImage: "https://commonsware.com/images/commonsware_logo.png", location: "Global" }
        ]
    },
    ios: {
        youtube: [
            { title: "Swift Programming Tutorial", url: "https://www.youtube.com/@SwiftProgrammingTutorial", description: "Concise guides for Swift & SwiftUI", location: "Global" },
            { title: "Sean Allen", url: "https://www.youtube.com/@seanallen", description: "Career tips & iOS interview prep", previewImage: "/assets/previews.png", location: "Global" }
        ],
        courses: [
            { title: "100 Days of SwiftUI", url: "https://www.hackingwithswift.com/100/swiftui", description: "Community-loved free course by Paul Hudson", previewImage: "https://www.hackingwithswift.com/img/100-days-of-swiftui.png", location: "Global" },
            { title: "iOS & Swift - Bootcamp", url: "https://www.udemy.com/course/ios-13-app-development-bootcamp/", description: "The classic full-stack iOS curriculum", previewImage: "/assets/previews.png", location: "Global" }
        ],
        podcasts: [
            { title: "Swift over Coffee", url: "https://www.hackingwithswift.com/podcast", description: "Relaxed discussion for the Apple ecosystem", previewImage: "/assets/previews.png", location: "Global" },
            { title: "Under the Radar", url: "https://www.relay.fm/undertheradar", description: "Independent app development and business", location: "Global" }
        ],
        websites: [
            { title: "Swift.org", url: "https://www.swift.org/", description: "Official source of the Swift revolution", previewImage: "https://www.swift.org/favicon.ico", location: "Global" },
            { title: "Hacking with Swift", url: "https://www.hackingwithswift.com/", description: "Massive library of tutorials and guides", previewImage: "/assets/previews.png", location: "Global" }
        ],
        projects: [
            { title: "Wikipedia iOS", url: "https://github.com/wikimedia/wikipedia-ios", description: "High-quality reference for deep app features", previewImage: "https://github.com/wikimedia/wikipedia-ios/raw/main/Wikipedia/Images/AppIcon.png", location: "Global" },
            { title: "Kickstarter iOS", url: "https://github.com/kickstarter/ios-oss", description: "Open source functional programming at scale", previewImage: "/assets/previews.png", location: "Global" }
        ],
        people: [
            { title: "Paul Hudson", url: "https://www.hackingwithswift.com/", description: "The creator of Hacking with Swift.", previewImage: "https://www.hackingwithswift.com/img/paul.jpg", location: "Global" },
            { title: "Sean Allen", url: "https://www.youtube.com/@seanallen", description: "Prolific iOS teacher and YouTuber.", previewImage: "/assets/previews.png", location: "Global" }
        ]
    }
};

export const COMMUNITY_RESOURCES: Record<string, CommunityResources> = {
    android: {
        meetups: [
            { title: "Auckland Android Community", url: "https://www.meetup.com/android-meetup/", description: "Technical talks and social networking.", location: "Auckland" },
            { title: "GDG Wellington", url: "https://www.meetup.com/gdg-wellington/", description: "Google Developer Group covering Android & Cloud.", location: "Wellington" },
            { title: "GDG Auckland", url: "https://www.meetup.com/gdg-auckland/", description: "Monthly meetings for Google tech enthusiasts.", location: "Auckland" }
        ],
        events: [
            { title: "DevFest Auckland", url: "https://devfest.gdgauckland.nz/", description: "The largest Google technology conference in NZ.", location: "Auckland" },
            { title: "Google I/O Extended NZ", url: "https://gdg.community.dev/", description: "Local events following Google's annual conference.", location: "NZ-Wide" },
            { title: "NZ Tech Rally", url: "https://nztechrally.tech/", description: "Community-driven software conference.", location: "Wellington" }
        ],
        localProjects: [
            { title: "Enro", url: "https://github.com/isaac-udy/enro", description: "Powerful navigation library by Isaac Udy.", previewImage: "https://github.com/isaac-udy/enro/raw/main/docs/logo.png", location: "Wellington" },
            { title: "NZ COVID Pass Verifier", url: "https://github.com/minhealthnz/nzcp-android", description: "Open source verification tool for NZ.", location: "NZ-Wide" },
            { title: "Open Source NZ", url: "https://github.com/RichardLitt/open-source-aotearoa-nz", description: "List of NZ-based open source projects.", location: "NZ-Wide" }
        ],
        celebrities: [
            { title: "Julius Spencer", url: "https://www.linkedin.com/in/juliusspencer/", description: "Founder of GDG Auckland, Director at JSA.", location: "Auckland" },
            { title: "Isaac Udy", url: "https://github.com/isaac-udy", description: "Android GDE, Platform Lead at ANZx.", previewImage: "https://avatars.githubusercontent.com/u/18519051?v=4", location: "Wellington" },
            { title: "Sam Hindmarsh", url: "https://hndmr.sh/", description: "GDG Wellington Organizer, Senior Android Engineer.", previewImage: "https://avatars.githubusercontent.com/u/2135075?v=4", location: "Wellington" },
            { title: "Dilum De Silva", url: "https://github.com/dilumdesilva", description: "GDG Auckland Organizer, Full-stack Engineer.", previewImage: "https://avatars.githubusercontent.com/u/23308182?v=4", location: "Auckland" },
            { title: "Matt Ranocchiari", url: "https://www.linkedin.com/in/matthewranocchiari/", description: "GDG Auckland & Wellington Organizer.", location: "Auckland/Wellington" }
        ]
    },
    ios: {
        meetups: [
            { title: "The Auckland iOS Meetup", url: "https://www.meetup.com/Auckland-iOS-Meetup/", description: "Monthly gatherings for iOS devs.", location: "Auckland" },
            { title: "CocoaHeads Auckland", url: "https://www.meetup.com/Auckland-CocoaHeads/", description: "Long-running community for Apple devs.", location: "Auckland" },
            { title: "CocoaHeads Wellington", url: "https://www.meetup.com/Wellington-CocoaHeads/", description: "Wellington based Apple developer group.", location: "Wellington" }
        ],
        events: [
            { title: "Dev Day NZ", url: "https://devday.io/", description: "NZ's premier developer conference.", location: "Auckland" },
            { title: "NZGDC", url: "https://nzgdc.com/", description: "NZ Game Developers Conference.", location: "NZ-Wide" }
        ],
        localProjects: [
            { title: "Open Source NZ", url: "https://github.com/RichardLitt/open-source-aotearoa-nz", description: "List of projects including mobile contributions.", location: "NZ-Wide" }
        ],
        celebrities: [
            { title: "Natalia Panferova", url: "https://nilcoalescing.com/", description: "SwiftUI expert, Co-founder of Nil Coalescing.", previewImage: "https://avatars.githubusercontent.com/u/15342922?v=4", location: "Alexandra" },
            { title: "Dan Too", url: "https://twitter.com/daniel_too", description: "Organizer of Auckland iOS Meetup.", location: "Auckland" },
            { title: "Stan Bykov", url: "https://www.linkedin.com/in/stanbykov/", description: "Organizer of CocoaHeads Auckland.", location: "Auckland" },
            { title: "Mitchell Johnson", url: "https://www.linkedin.com/in/mjohnsonnz/", description: "Organizing CocoaHeads Auckland and lead at ANZ.", location: "Auckland" }
        ]
    }
};
