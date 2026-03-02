export interface Resource {
    title: string;
    url: string;
    description: string;
    previewImage?: string; // Optional thumbnail/preview image
    imageUrl?: string;     // Direct image link for the resource
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
            { title: "Android Developers", url: "https://www.youtube.com/@AndroidDevelopers", description: "Official Android developer channel", previewImage: "https://www.gstatic.com/android/devsite/56d0d5d5a9d4e5d5/android/images/lockup_android_developers.png" },
            { title: "Philipp Lackner", url: "https://www.youtube.com/@PhilippLackner", description: "Excellent Jetpack Compose and architecture tutorials", previewImage: "/assets/previews.png" }
        ],
        courses: [
            { title: "Android Basics with Compose", url: "https://developer.android.com/courses/android-basics-compose/course", description: "Official Google course for beginners", previewImage: "https://developer.android.com/static/images/courses/android-basics-compose/banner.png" },
            { title: "Developing Android Apps with Kotlin", url: "https://www.udacity.com/course/developing-android-apps-with-kotlin--ud9012", description: "Comprehensive Udacity course" }
        ],
        podcasts: [
            { title: "Android Faithful", url: "https://androidfaithful.com/", description: "Weekly news and analysis for Android enthusiasts", previewImage: "/assets/previews.png" },
            { title: "Fragmented", url: "https://fragmentedpodcast.com/", description: "An Android developer podcast" }
        ],
        websites: [
            { title: "Android Developers Blog", url: "https://android-developers.googleblog.com/", description: "Latest news from the Android team" },
            { title: "Kotlin Documentation", url: "https://kotlinlang.org/docs/home.html", description: "Official Kotlin language guide", previewImage: "/assets/previews.png" }
        ],
        projects: [
            { title: "Now in Android", url: "https://github.com/android/nowinandroid", description: "A fully functional Android app built entirely with Kotlin and Jetpack Compose", previewImage: "https://github.com/android/nowinandroid/raw/main/tools/screenshots/main.png" },
            { title: "NZ COVID Pass Verifier", url: "https://github.com/minhealthnz/nzcp-android", description: "Official NZ Ministry of Health COVID pass verifier (Open Source)" }
        ],
        people: [
            { title: "Yahia R. (Auckland)", url: "https://www.upwork.com/freelancers/~017c185e49f874c7e6", description: "Mobile Engineer with 10+ years experience in iOS, Android, and Flutter" },
            { title: "Leviticus David H. (Te Anau)", url: "https://www.upwork.com/freelancers/~014d8b67f1b297c4f1", description: "Self-taught developer with experience in Android and web applications" }
        ]
    },
    ios: {
        youtube: [
            { title: "Swift Programming Tutorial", url: "https://www.youtube.com/@SwiftProgrammingTutorial", description: "Great for Swift and SwiftUI basics" },
            { title: "Sean Allen", url: "https://www.youtube.com/@seanallen", description: "iOS development tips and career advice", previewImage: "/assets/previews.png" }
        ],
        courses: [
            { title: "100 Days of SwiftUI", url: "https://www.hackingwithswift.com/100/swiftui", description: "Free comprehensive course by Paul Hudson", previewImage: "https://www.hackingwithswift.com/img/100-days-of-swiftui.png" },
            { title: "iOS & Swift - Bootcamp", url: "https://www.udemy.com/course/ios-13-app-development-bootcamp/", description: "Popular Udemy course by Angela Yu" }
        ],
        podcasts: [
            { title: "Swift over Coffee", url: "https://www.hackingwithswift.com/podcast", description: "Weekly news and discussion about Swift", previewImage: "/assets/previews.png" },
            { title: "Under the Radar", url: "https://www.relay.fm/undertheradar", description: "From the developers of Overcast and Castro" }
        ],
        websites: [
            { title: "Swift.org", url: "https://www.swift.org/", description: "Official Swift language site", previewImage: "https://www.swift.org/favicon.ico" },
            { title: "Hacking with Swift", url: "https://www.hackingwithswift.com/", description: "Incredibly deep resource for all things iOS" }
        ],
        projects: [
            { title: "Wikipedia iOS", url: "https://github.com/wikimedia/wikipedia-ios", description: "The official Wikipedia iOS app", previewImage: "https://github.com/wikimedia/wikipedia-ios/raw/main/Wikipedia/Images/AppIcon.png" },
            { title: "Kickstarter iOS", url: "https://github.com/kickstarter/ios-oss", description: "Kickstarter's open source iOS app" }
        ],
        people: [
            { title: "Sam Jarman", url: "https://samjarman.co.nz/", description: "iOS Developer, speaker, and prominent NZ tech community member" },
            { title: "Daniel Too", url: "https://twitter.com/daniel_too", description: "Organizer of The Auckland iOS Meetup" }
        ]
    }
};

export const COMMUNITY_RESOURCES: Record<string, CommunityResources> = {
    android: {
        meetups: [
            { title: "Auckland Android Community", url: "https://www.meetup.com/auckland-android-community/", description: "Regular technical talks and social networking (1100+ members)" },
            { title: "GDG Wellington", url: "https://www.meetup.com/gdg-wellington/", description: "Google Developer Group covering Android, Cloud, and more" },
            { title: "GDG Christchurch", url: "https://gdg.community.dev/gdg-christchurch/", description: "Google Developer Group in the South Island" }
        ],
        events: [
            { title: "Android Dev Summit", url: "https://developer.android.com/dev-summit", description: "Official annual technical event for Android developers" },
            { title: "Google I/O Aotearoa (Extended)", url: "https://gdg.community.dev/", description: "Local 'Extended' events following Google's annual developer conference" }
        ],
        localProjects: [
            { title: "NZ COVID Pass Verifier", url: "https://github.com/minhealthnz/nzcp-android", description: "Open source verification tool for NZ COVID passes" },
            { title: "Open Source Aotearoa NZ", url: "https://github.com/RichardLitt/open-source-aotearoa-nz", description: "Curated list of NZ-based open source projects" }
        ],
        celebrities: [
            { title: "Yahia R.", url: "https://www.upwork.com/freelancers/~017c185e49f874c7e6", description: "Prominent mobile engineer based in Auckland" },
            { title: "Leviticus David H.", url: "https://www.upwork.com/freelancers/~014d8b67f1b297c4f1", description: "Active contributor from Te Anau" }
        ]
    },
    ios: {
        meetups: [
            { title: "The Auckland iOS Meetup", url: "https://www.meetup.com/Auckland-iOS-Meetup/", description: "Monthly gatherings for iOS developers and designers" },
            { title: "CocoaHeads Wellington", url: "https://www.meetup.com/Wellington-CocoaHeads/", description: "Long-running community for Apple platform developers" },
            { title: "Auckland Mobile Developer Meetup", url: "https://www.meetup.com/auckland-mobile-developer-meetup/", description: "Cross-platform mobile community including iOS" }
        ],
        events: [
            { title: "Dev Day Aotearoa", url: "https://devday.io/", description: "NZ's premier developer conference (Auckland-based, NZ-wide impact)" },
            { title: "NZGDC", url: "https://nzgdc.com/", description: "NZ Game Developers Conference - includes mobile/iOS dev interest" },
            { title: "NZ Tech Rally", url: "https://www.nztechrally.tech/", description: "Community-driven software conference in Wellington" }
        ],
        localProjects: [
            { title: "Wikipedia iOS (Global/Local)", url: "https://github.com/wikimedia/wikipedia-ios", description: "Used by many NZ devs for reference" },
            { title: "Open Source Aotearoa NZ", url: "https://github.com/RichardLitt/open-source-aotearoa-nz", description: "List of projects including mobile contributions" }
        ],
        celebrities: [
            { title: "Sam Jarman", url: "https://samjarman.co.nz/", description: "Prolific iOS speaker and technical writer" },
            { title: "Kier Draven", url: "https://twitter.com/kierdraven", description: "Organizer of Auckland Mobile Developer Meetup" }
        ]
    }
};
