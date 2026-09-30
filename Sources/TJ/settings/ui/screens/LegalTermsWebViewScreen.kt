package com.tpsk.tj.settings.ui.screens

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalTermsWebViewScreen(
    url: String = "https://firstappserver.fly.dev/privacy",
    title: String = "Master Terms of Service & EULA",
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var loadError by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()

    val legalHtmlContent = remember(isDark) {
        val bg = if (isDark) "#121212" else "#FFFFFF"
        val text = if (isDark) "#E0E0E0" else "#212121"
        val cardBg = if (isDark) "#1E1E1E" else "#F5F5F5"
        val accent = "#1D9BF0"

        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                    background-color: $bg;
                    color: $text;
                    margin: 0;
                    padding: 20px;
                    line-height: 1.6;
                }
                h1 { color: $accent; font-size: 22px; margin-bottom: 6px; font-weight: 800; }
                h2 { color: $accent; font-size: 17px; margin-top: 22px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 6px; font-weight: 700; }
                h3 { font-size: 15px; margin-top: 16px; font-weight: 700; }
                p, li { font-size: 13.5px; opacity: 0.92; }
                .card {
                    background-color: $cardBg;
                    border-radius: 12px;
                    padding: 16px;
                    margin: 16px 0;
                    border-left: 4px solid $accent;
                }
                .highlight { color: #FF5252; font-weight: bold; }
                ul, ol { padding-left: 20px; }
            </style>
        </head>
        <body>
            <h1>MASTER TERMS OF SERVICE AND END USER LICENSE AGREEMENT</h1>
            <p><strong>Last Revised:</strong> 24 September 2026</p>
            <p><strong>Application Identity:</strong> TJ Super App / Network ("TJ," "Platform," "Application," "we," "us," or "our")</p>
            <p><strong>Primary Entity:</strong> TPSK (Pty) Ltd, incorporated under the laws of the Republic of South Africa.</p>

            <div class="card">
                <span class="highlight">CRITICAL NOTICE: ZERO TOLERANCE FOR OBJECTIONABLE CONTENT</span>
                <p>FirstApp maintains an absolute zero-tolerance policy regarding objectionable, abusive, harassing, hate speech, explicit, or illegal User Generated Content (UGC). Violators will be immediately suspended or permanently banned, and offending content will be purged within 24 hours.</p>
            </div>

            <h2>1. Contractual Framework, Scope, and Hierarchy</h2>
            <p><strong>1.1. Binding Agreement.</strong> These Master Terms of Service and End User License Agreement ("Terms") establish an enforceable legal contract between you ("User," "you," or "your") and the Company. By installing, creating an account, accessing, or utilizing any feature of the Platform, you acknowledge that you have read, understood, and agreed to be bound by these Terms and our incorporated Privacy and Telemetry Policy.</p>
            <p><strong>1.2. Architecture and Modularity.</strong> The Platform functions as a modular super-application aggregating multiple distinct communication, content, social discovery, economic, and logistical verticals into a unified graphical interface and account database. The core modules include:</p>
            <ul>
                <li><strong>Chat:</strong> Direct peer-to-peer messaging, file transfers, and ephemeral text and media stories.</li>
                <li><strong>Channels:</strong> Group, thematic, and broad-scale community broadcast streams.</li>
                <li><strong>Jobs:</strong> Career postings, employment discovery, and vocational connections.</li>
                <li><strong>Rewards:</strong> A gamified engagement system delivering promotional credits for viewing ads, daily specials, and partner offers.</li>
                <li><strong>Social:</strong> Micro-blogging, text/image publishing, community feeds, and profile aggregation.</li>
                <li><strong>Scroll:</strong> Short-form, full-screen vertical video content consumption, distribution, and discovery.</li>
                <li><strong>Mingle:</strong> Real-time geolocation-based personal profiling, proximity scanning, and social discovery.</li>
                <li><strong>Wallet:</strong> Integrated stored-value balances, internal payment tracking, and ledger facilities.</li>
                <li><strong>Roadmap Deployments:</strong> Pre-release verticals marked as "(Soon)" including Vocab (educational modules), Stream (multimedia/music hosting), and Ride (on-demand transport dispatch).</li>
            </ul>
            <p><strong>1.3. Precedence.</strong> In the event of a direct, irreconcilable conflict between this Master Agreement and any vertical-specific operational rules, the terms governing that specific operational vertical shall control solely with respect to that vertical.</p>

            <h2>2. Eligibility, Account Registration, and Master Identity</h2>
            <p><strong>2.1. Legal Age and Capacity.</strong> Access to the general Platform requires that you be at least 18 years of age (or the legal age of majority in your jurisdiction of residence). Use of the Mingle (social discovery) module and the Wallet transaction rails is strictly limited to individuals aged 18 and older. By accessing these specific modules, you represent and warrant under penalty of perjury that you meet this threshold.</p>
            <p><strong>2.2. Single Identity and Profile Authenticity.</strong> You may maintain only one Master Account tied to your verified mobile number and device profile. You agree to provide true, accurate, current, and complete information across all setup forms, including your public display name, biological or chosen age, gender identification, and bio attributes. Account creation through automated bots, scrapers, emulators, or falsified identities is strictly prohibited. Impersonation of another human being, celebrity, entity, or corporate brand within "Social," "Mingle," or "Channels" is cause for immediate account expulsion without prior warning.</p>
            <p><strong>2.3. Account Custody and Credential Security.</strong> You are solely liable for safeguarding your access credentials, passcodes, and multi-factor authentication codes. Any activity, communication, or transaction originating from your device or authenticated token will be legally attributed to you until you have formally alerted our Trust & Safety department of unauthorized compromise.</p>

            <h2>3. Detailed Service Modules and Operational Rules</h2>
            <p><strong>3.1. Messaging ("Chat"), Channels, and Stories.</strong> The Platform provides messaging tools allowing end-to-end and platform-routed messaging, media distribution, text-based ephemeral stories, and media-based stories. Stories published under "Media story" or "Text story" display on the Platform for 24 hours before automatic sunsetting unless manually archived or deleted prior. Creators/moderators of broadcast Channels bear exclusive legal liability for all material posted. We reserve the right to review flagged communications, automated metadata, and reported accounts to identify violations of criminal statutes or safety standards.</p>
            <p><strong>3.2. Public Feeds ("Social") and Short-Form Video ("Scroll").</strong> You retain raw copyright ownership in authentic photos, texts, commentary, and video material you upload. However, by publishing content onto any public area of the Platform, you grant the Company a perpetual, worldwide, irrevocable, non-exclusive, fully paid-up, royalty-free, sublicensable license to host, transcode, cache, distribute, reproduce, index, display, and publicly perform such content across our server networks and delivery partners. Accounts receiving multiple validated copyright infringement notices will be permanently disabled. Content depicting graphic violence, CSAM, non-consensual sexual material, animal abuse, self-harm instructions, or coordinated criminal action will be removed immediately upon detection, with source IP logs preserved for law enforcement hand-off.</p>
            <p><strong>3.3. Location-Based Social Discovery ("Mingle").</strong> The Mingle module requires background and foreground telemetry calculation. By toggling on Mingle, you explicitly consent to the continuous processing of your geographic coordinates and the display of your relative distance to other registered users within customized filter settings (from 1 km to 100+ km). Your selected Mingle display name, uploaded profile photographs (up to 5), age, bio, and gender preference selections will be served publicly to nearby users within matching criteria. You assume all responsibility, risk, and liability for all offline, in-person meetings, physical appointments, or private engagements resulting from connections made via Mingle or Chat. Commercial solicitation, sex work, stalking, fake profiles, or coordinate-spoofing utilities will trigger an immediate permanent ban.</p>
            <p><strong>3.4. Rewards Program and Gamified Advertising.</strong> The Rewards ecosystem allows users to accrue virtual engagement points ("Points") by completing voluntary daily platform micro-tasks. Points are strictly internal, non-fungible, promotional units with no cash equivalent, property value, or statutory protections. Automated click scripts, video-looping bots, macro apps, or ad-blocking bypass tools will cause instantaneous forfeiture of all accumulated points and immediate account deletion.</p>
            <p><strong>3.5. Vocational Listings ("Jobs").</strong> The "Jobs" portal functions solely as a notice board connecting job seekers with prospective employers or clients. The Company is not an employer, recruitment agency, labor broker, or employment contractor. Users must exercise independent due diligence before signing contracts or disclosing sensitive personal or financial information.</p>
            <p><strong>3.6. Financial Rails ("Wallet").</strong> The In-App Wallet allows users to hold stored-value balances, facilitate platform settlements, and execute internal micro-transactions. We utilize regulated financial services providers and licensed payment processors for custodial services. Wallet balances do not earn interest and are not commercial bank deposits insured by statutory deposit guarantees.</p>
            <p><strong>3.7. Roadmap Verticals ("Vocab," "Stream," "Ride").</strong> Functionality marked as "(Soon)"—including vocabulary learning modules ("Vocab"), music and media streaming ("Stream"), and transit dispatch services ("Ride")—is non-operational and provided for informational UI planning purposes only.</p>

            <h2>4. Code of Conduct and Network Abuse</h2>
            <p>You agree and covenant that you will NOT: decompile, reverse engineer, disassemble, decrypt, or attempt to derive underlying source code or system algorithms; interfere with, overload, flood, or mount DoS attacks against our infrastructure; deploy spiders, scrapers, or data-mining scripts; probe or scan API vulnerability barriers; bypass geolocation filters through coordinate-spoofing utilities; or upload computer viruses, Trojan horses, or malicious payloads.</p>

            <h2>5. Intellectual Property and Proprietary Rights</h2>
            <p><strong>5.1. Exclusive Platform Ownership.</strong> The TJ logo, platform software, graphical layout, iconography, user flows, database structures, sound marks, and UX configurations are the exclusive intellectual property of the Company and its technology licensors.</p>
            <p><strong>5.2. Limited Personal License.</strong> We grant you a personal, non-exclusive, non-transferable, revocable, and limited license to download, install, and execute the Application on your authorized personal handheld mobile device for non-commercial consumer use.</p>

            <h2>6. Indemnification and Comprehensive Release</h2>
            <p>You agree to defend, indemnify, and hold completely harmless the Company, its holding entities, subsidiaries, affiliates, directors, officers, shareholders, employees, legal advisors, and agents from and against all legal claims, liabilities, lawsuits, damages, costs, awards, losses, and expenses arising out of or directly related to your access to or misuse of the Platform, User Content, or off-platform real-world meetings arranged via Mingle or Chat.</p>

            <h2>7. Warranty Exclusions and Limitation of Liability</h2>
            <p><strong>7.1. Absolute "As-Is" Disclaimer.</strong> THE APPLICATION, INCLUDING ALL SUB-SYSTEMS, MODULES, CONTENT DIRECTORIES, PAYMENT INTERFACES, DISCOVERY RADIUS CALCULATORS, AND REWARDS APIS, IS DELIVERED ON AN "AS-IS" AND "AS-AVAILABLE" BASIS TO THE MAXIMUM EXTENT PERMITTED UNDER APPLICABLE LAW.</p>
            <p><strong>7.2. Exclusion of Consequential Damages.</strong> UNDER NO CIRCUMSTANCES SHALL THE COMPANY, ITS AFFILIATES, OFFICERS, DIRECTORS, OR LICENSORS BE LIABLE TO YOU OR ANY THIRD PARTY FOR ANY INDIRECT, CONSEQUENTIAL, INCIDENTAL, SPECIAL, EXEMPLARY, OR PUNITIVE DAMAGES.</p>
            <p><strong>7.3. Financial Liability Cap.</strong> IN NO EVENT SHALL THE TOTAL COMBINED AGGREGATE LIABILITY OF THE COMPANY FOR ALL CLAIMS EXCEED THE LESSER OF: (A) THE TOTAL NET PLATFORM FEES RETAINED BY THE COMPANY DIRECTLY FROM YOUR INDIVIDUAL ACCOUNT TRANSACTIONS DURING THE THREE (3) MONTHS IMMEDIATELY PRECEDING THE CLAIM; OR (B) AN AGGREGATE CEILING OF 500 SOUTH AFRICAN RAND (ZAR 500).</p>

            <h2>8. Term, Account Cancellation, and Forfeiture</h2>
            <p><strong>8.1. Termination by User.</strong> Complete deletion of profile entries requires processing through the in-app account deletion utility located in settings (<strong>Settings &rarr; Account Settings &rarr; Delete Account</strong>).</p>
            <p><strong>8.2. Administrative Suspension.</strong> The Company retains absolute discretion to instantly suspend, throttle, freeze, or terminate your Master Account if we suspect any violation of these Terms, safety rules, or platform integrity standards.</p>

            <h2>9. Dispute Resolution, Governing Law, and Jurisdiction</h2>
            <p><strong>9.1. Governing Law.</strong> These Terms shall be governed exclusively by the laws of the Republic of South Africa, without regard to conflict of law principles.</p>
            <p><strong>9.2. Forum Selection.</strong> Any legal suit, action, or proceeding arising out of or related to these Terms or the Platform shall be instituted exclusively in the High Court of South Africa (Gauteng Division, Johannesburg).</p>

            <h2>10. General and Administrative Provisions</h2>
            <p><strong>10.1. Modifications to Terms.</strong> We reserve the right to revise, update, replace, or modify these Terms at our discretion.</p>

            <h2>11. Legal Department and Contact Information</h2>
            <div class="card">
                <p><strong>TPSK (Pty) Ltd & Legal Operations</strong><br>
                <strong>Platform:</strong> TJ Super App / Network<br>
                <strong>Representative:</strong> Tshidiso Khoza<br>
                <strong>Email:</strong> tshidisokhoza1992@gmail.com<br>
                <strong>Phone / Support:</strong> +27 63 265 9938 (0632659938)<br>
                <strong>Physical Office:</strong> Pretoria, Gauteng, Republic of South Africa</p>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            loadError = false
                            isLoading = true
                            if (url.startsWith("https://firstappserver.fly.dev") || url.contains("privacy") || url.contains("terms")) {
                                webViewInstance?.loadDataWithBaseURL("https://firstappserver.fly.dev/", legalHtmlContent, "text/html", "UTF-8", null)
                            } else {
                                webViewInstance?.reload()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        webViewInstance = this
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    loadError = true
                                    isLoading = false
                                    view?.loadDataWithBaseURL("https://firstappserver.fly.dev/", legalHtmlContent, "text/html", "UTF-8", null)
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResponse: android.webkit.WebResourceResponse?
                            ) {
                                super.onReceivedHttpError(view, request, errorResponse)
                                if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 400) {
                                    loadError = true
                                    isLoading = false
                                    view?.loadDataWithBaseURL("https://firstappserver.fly.dev/", legalHtmlContent, "text/html", "UTF-8", null)
                                }
                            }
                        }

                        // Load formatted legal document directly to prevent server 404s
                        if (url.startsWith("https://firstappserver.fly.dev") || url.contains("privacy") || url.contains("terms")) {
                            loadDataWithBaseURL("https://firstappserver.fly.dev/", legalHtmlContent, "text/html", "UTF-8", null)
                        } else {
                            loadUrl(url)
                        }
                    }
                },
                update = { webView ->
                    webViewInstance = webView
                }
            )

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
