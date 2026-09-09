UI Design System
Scope: Pure visual/UI specification, reverse-engineered from an existing reference application, intended to be applied to a new/different product. Colors, spacing, typography, radius, shadows, icons, animation, and visual interaction states (hover/active/focus/disabled) only. Component logic, state management, API calls, localStorage keys, and accessibility-attribute audits are intentionally excluded. Anything not directly confirmed in the reference source is marked "Requires manual verification." All naming below has been genericized — no product name, company name, or brand-specific asset names from the reference application are used.

1. Executive Summary
Design philosophy: restrained, enterprise SaaS visual language — white surfaces, one branded blue accent, generous whitespace, small (13–14px) body type, low-elevation blue-tinted shadows. Dominant composition: fixed left sidebar + sticky top bar + centered max-width content column.

Two visual sub-styles coexist:

Default app chrome — quiet zinc grays, blue brand accent, small type, subtle shadows (used almost everywhere).
"Marketing hero" outlier — seen in exactly one settings-panel screen in the reference app: heavy uppercase tracking-widest labels, italic subtext, dark zinc-900 CTA blocks with oversized low-opacity background icons. Not the dominant pattern.
Visual identity:

Browser theme-color meta value: #0057B8 (brand primary color)
Logo: single raster logo image, used in sidebar header and login screen
Iconography: exclusively line-style (Lucide), strokeWidth 1.5–1.75 typical
No illustrations, no mascot/playful iconography — professional and data-dense
Core recurring visual rules:

Border-radius: rounded but not pill-shaped for containers (8–16px); fully rounded (rounded-full) reserved for dots, avatars, spinners
Shadows: subtle and blue-tinted (rgba(0,87,184,...)), not neutral gray
Icon sizing: controlled exclusively via the Lucide size prop — no w-4 h-4-style icon sizing anywhere
No dark mode anywhere in the app
2. Technology Stack (styling-relevant only)
Category	Library	Version
CSS framework	tailwindcss	^3.4.17
CSS processing	postcss / autoprefixer	^8.4.49 / ^10.4.20
Icons	lucide-react	^1.14.0
Charts	recharts	^3.8.1
Animation	framer-motion	^12.38.0
Toasts	react-toastify	^11.0.5
Class utilities	clsx / tailwind-merge	^2.1.1 / ^3.5.0
QR codes	qrcode.react	^4.2.0
Syntax highlighting	react-syntax-highlighter	^16.1.1
Not present: no component library (MUI/Ant/Chakra/shadcn/Radix), no CSS Modules, no styled-components/Emotion/Stitches, no Sass/Less. Plain CSS + Tailwind utility classes only.

Theming: No React theme provider exists. All theming is CSS custom properties in :root (in the global stylesheet), consumed by Tailwind via var(--token, fallback). No dark mode implementation of any kind — no dark: variants, no prefers-color-scheme, no theme toggle.

Animation plugin discrepancy: Components use slide-in-from-top-1, slide-in-from-top-2, slide-in-from-bottom-4 class names (matching the tailwindcss-animate plugin's naming convention), but that plugin is not installed (the Tailwind config's plugins: [] is empty) and these classes are not hand-defined in index.css either. Only .animate-in, .fade-in, .zoom-in, .slide-in-up are actually defined. The directional/distance variants may be inert. Flagged, not assumed to work.

3. Design Tokens
3.1 Color Tokens (CSS custom properties, the global stylesheet)
--brand-primary:         #0057B8;
--brand-primary-dark:    #003E8A;
--brand-primary-light:   #E8F0FB;
--brand-accent:         #F5A300;
--brand-accent-light:   #FEF3CD;

--color-primary:       var(--brand-primary);
--color-primary-hover: var(--brand-primary-dark);
--color-primary-light: var(--brand-primary-light);

--color-accent:        var(--brand-accent);
--color-accent-green:  #22c55e;
--color-accent-red:    #ef4444;
--color-accent-amber:  #f59e0b;

--color-bg-main:       #F3F6FB;
--color-bg-card:       #ffffff;

--color-text-main:     #0D1B2E;
--color-text-muted:    #5A6A7E;
--color-text-dim:      #94A3B8;

--color-border-subtle: #EAF0F8;
--color-border-main:   #D5E0EE;
Token	Hex	Visual usage
--brand-primary	#0057B8	Primary buttons, active nav indicator, focus glow, links, theme-color meta
--brand-primary-dark	#003E8A	Primary button hover/active
--brand-primary-light	#E8F0FB	Nav hover/active bg, table row hover, modal footer bg, pill backgrounds
--brand-accent	#F5A300	Confirmed single deliberate usage: login-screen headline accent word (see §18). Otherwise unconfirmed elsewhere
--brand-accent-light	#FEF3CD	No confirmed usage site
--color-accent-green	#22c55e	Success states
--color-accent-red	#ef4444	Error/critical/logout
--color-accent-amber	#f59e0b	Warning states
--color-bg-main	#F3F6FB	App/page background
--color-bg-card	#ffffff	Card/surface background
--color-text-main	#0D1B2E	Primary text, headings
--color-text-muted	#5A6A7E	Secondary/body text
--color-text-dim	#94A3B8	Tertiary/label text, uppercase micro-labels
--color-border-subtle	#EAF0F8	Hairline dividers, card borders, row borders
--color-border-main	#D5E0EE	Stronger borders (sidebar, modal, card-flat)
3.2 Layout Dimension Tokens
--sidebar-width: 256px;
--topbar-height: 56px;
3.3 Severity Color System (three divergent definitions coexist)
A. Badge/dot utility classes (index.css, Tailwind built-in palette):

Class	Background	Text
.badge-critical	bg-red-50	text-red-600
.badge-high	bg-orange-50	text-orange-600
.badge-medium	bg-amber-50	text-amber-700
.badge-low	bg-blue-50	text-blue-600
.badge-info	bg-zinc-100	text-zinc-600
.badge-success	bg-green-50	text-green-600
Dot class	Color
.sev-dot-critical	bg-red-500
.sev-dot-high	bg-orange-500
.sev-dot-medium	bg-amber-400
.sev-dot-low	bg-blue-500
.sev-dot-info	bg-zinc-400
B. the vulnerability/trend chart component SEV_COLORDS (hex, Recharts fills — typo in source object name preserved):

CRITICAL: #F43F5E   HIGH: #F59E0B   MEDIUM: #10B981   LOW: #3B82F6   INFO: #64748B
Diverges from Set A (e.g. MEDIUM is emerald here vs. amber in badges; CRITICAL is rose vs. red-600).

C. the detail-modal component/the main data-table page severity config objects — independently authored per-file Tailwind-class maps ({color, bg, text, border, icon} shape), not shared. Exact color values: requires manual verification.

Implication for reproduction: pick ONE severity palette; do not assume the three are consistent — they are not.

3.4 Shadow Tokens
--shadow-card:    0 4px 24px rgba(0,87,184,0.06);
--shadow-premium: 0 8px 32px rgba(0,87,184,0.10);
--shadow-modal:   0 0 0 1px #D5E0EE, 0 16px 48px rgba(0,0,0,0.12);
Card/premium shadows are blue-tinted, not neutral — a deliberate, consistent choice. Modal shadow uses a neutral black component plus a simulated 1px border. Mapped in the Tailwind config as shadow-card / shadow-premium / shadow-modal.

3.5 Border Radius
borderRadius: { card: '12px', btn: '8px' }
In practice raw Tailwind radius utilities dominate:

Context	Radius
Buttons	rounded-lg (8px)
Cards (.card-premium/.card-flat)	12px
Modal content	14px
Login-panel inputs/buttons	rounded-xl (12px)
Badges	rounded-md (6px)
Nav items	8px
Empty-state icon well	rounded-2xl (16px)
Severity/status dots, avatars	rounded-full
3.6 Typography Tokens
fontSize: { '2xs': ['10px', { lineHeight: '14px', letterSpacing: '0.04em' }] }
Only custom size token; nearly all text sizing instead uses arbitrary bracket values (text-[11px], text-[13px], etc.) directly in class strings.

Base body (index.css):

font-family: 'Inter', 'SF Pro Display', system-ui, -apple-system, sans-serif;
font-size: 14px;
line-height: 1.5;
letter-spacing: -0.008em;
-webkit-font-smoothing: antialiased;
Headings (h1,h2,h3,h4 base rule): letter-spacing: -0.03em; font-weight: 600; color: var(--color-text-main).

Observed text-size scale in practice:

Size	Usage
text-[9px]	Smallest pill labels
text-[10px]	Nav section titles, stat sub-labels
text-[11px]	Badges, stat labels, table headers, form errors
text-[13px]	Default body/button/table-cell text — most common
text-[14px]/text-sm	General body, compact titles
text-[15px]	Larger inline headings
text-[16px]	Default empty-state title
text-[18px]	Modal titles, section headers
text-[20px]	Page title
text-[22px]/text-[26px]/text-[32px]	Login-screen OTP entry, page title, hero headline respectively
text-2xl	Stat card values
Letter-spacing convention: negative tracking (-0.01em to -0.03em) on headings/titles/buttons; positive tracking (tracking-wider/tracking-widest) exclusively on uppercase micro-labels.

Font weights: font-medium (500, body emphasis/nav), font-semibold (600, headings/titles/table headers), font-bold (700, stat values, marketing-style headers), font-black (900, login-screen hero headline only — the single heaviest weight in the app).

3.7 Spacing / Fixed Dimensions
Sidebar width: 256px; sidebar header height: 64px
Topbar height: 56px
Page body padding: 32px; max-width 1400px
Nav item height: 38px; logout button height: 36px
Login form inputs: h-11 (44px); OTP-code inputs: h-14 (56px, tallest input in the app)
Auth card reference (legacy .auth-card CSS class, may be unused — see §18): max-width 380px, padding 36px 32px
3.8 Grid System
No custom grid framework. Plain Tailwind grid grid-cols-N + Flexbox. Observed pattern: grid grid-cols-1 md:grid-cols-3 (single column below 768px, 3 columns at/above).

3.9 Container Widths
Page content: max-width: 1400px
Modal default: max-w-md (~448px), overridden per-modal
Login form column: max-w-[420px]
EmptyState message: max-w-xs; PageError card: max-w-sm
3.10 Z-Index Scale
Value	Element
z-40	Sidebar
z-30	Sticky top bar
z-[100]	Modal overlay
9999 (toast custom property)	Toast notifications
3.11 Animation Durations
.duration-200 { animation-duration: 200ms; }
.duration-300 { animation-duration: 300ms; }
.duration-500 { animation-duration: 500ms; }
.duration-700 { animation-duration: 700ms; }
Plus keyframe defaults: fade-in = 250ms, zoom-in = 200ms, slide-in-up = 220ms.

3.12 Animation Easing
.animate-in base: cubic-bezier(0.16, 1, 0.3, 1) — the one deliberate custom "ease-out-expo"-style curve, reused identically in framer-motion transitions on the login screen (ease: [0.16, 1, 0.3, 1]) — confirmed as the app's one consistent "premium" easing signature.
Elsewhere: Tailwind's implicit default (cubic-bezier(0.4, 0, 0.2, 1)) applies to unclassed transition-* utilities (the vast majority).
One inline exception: the gauge widget gauge arc uses ease-out directly.
4. Layout System
4.1 Application Shell
<div className="layout">              flex, min-h-screen, bg = --color-bg-main
  <aside className="sidebar">         fixed, 256px, full height, white, right border
    <div className="sidebar-header">  64px
    <nav className="nav-links">       scrollable, 28px gap between sections
    <div className="sidebar-footer">  account links + logout
  </aside>
  <div className="main-content">      margin-left: 256px
    <header className="page-header">  sticky, 56px, white, bottom border+shadow
    <main className="page-body">      padding 32px, max-width 1400px
4.2 Top Bar
Height 56px, sticky, white background, bottom border (--color-border-subtle) plus a 1px box-shadow duplicate of that border for extra definition. Padding 0 32px.

4.3 Content Area
Single-column, max-width: 1400px, 32px padding all sides.

4.4 Card Layout
.card-premium {
  background: #ffffff;
  border: 1px solid var(--color-border-subtle);
  border-radius: 12px;
  box-shadow: var(--shadow-card);
  transition: box-shadow 0.2s ease;
}
.card-premium:hover { box-shadow: var(--shadow-premium); }

.card-flat {
  background: #ffffff;
  border: 1px solid var(--color-border-main);
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.02);
}
.card-premium escalates shadow on hover; .card-flat is static (individual components may add their own hover:border-zinc-300).

4.5 Modals
Two visual patterns coexist: a generic portal-based modal using shared .modal-* classes (header/body/footer slots, no entrance animation), and bespoke full-custom modals with their own markup that DO use entrance animation (animate-in fade-in slide-in-from-top-2 duration-150). This is a real inconsistency, not a documentation gap.

4.6 Scrollable Regions
Sidebar nav: vertical scroll, scrollbar visually hidden (scrollbar-width: none + ::-webkit-scrollbar { display: none })
Modal body: overflow-y-auto, overscroll-contain
Global custom scrollbar (everywhere else): 5px wide/tall, transparent track, --color-border-main thumb, darkens to --color-text-dim on hover
4.7 Sticky/Fixed Elements
Top bar: position: sticky; top: 0. Sidebar: position: fixed.

4.8 Overflow Handling
Sidebar container: overflow: hidden (inner nav scrolls independently). Nav labels: overflow: hidden; text-overflow: ellipsis; white-space: nowrap. Modal content: max-h-[90vh] cap with internal scroll.

5. Sidebar Specification
Property	Value
Width	256px fixed (no collapse mode)
Header height	64px
Background	#ffffff
Border	1px solid var(--color-border-main), right side only
Shadow	2px 0 12px rgba(0,87,184,0.04)
Position	fixed; top:0; left:0, z-index: 40
Section gap	28px
Section padding	20px 10px
Section title style	10px, weight 700, --color-text-dim, uppercase, letter-spacing: 0.15em
Nav item height	38px, padding 0 12px, radius 8px, icon-label gap 10px
Nav item font	13px, weight 500, letter-spacing: -0.01em
Nav item default color	--color-text-muted
Nav item hover	text/icon → --brand-primary, background → --brand-primary-light
Nav item active	text/icon → --brand-primary, weight 600, background --brand-primary-light, plus a 3px-wide rounded left indicator bar (inset 6px top/bottom)
Nav item transition	all 0.15s ease
Icon size	15px, strokeWidth: 1.75
Icon color	--color-text-dim default, --brand-primary on hover/active
Footer background	#FAFCFF (distinct off-white)
Footer padding	12px 10px, top border --color-border-subtle
Footer separator	1px solid --color-border-subtle, margin 6px 2px
Logout button	height 36px, color #EF4444, transparent bg; hover rgba(239,68,68,0.06); active scale(0.98)
No collapse/expand animation exists — sidebar has no collapsed state. Nav scrollbar is visually hidden cross-browser.

6. Header (Top Bar) Specification
Property	Value
Height	56px
Padding	0 32px
Background	#ffffff
Border	bottom, 1px solid var(--color-border-subtle)
Shadow	0 1px 0 var(--color-border-subtle)
Position	sticky; top:0; z-index:30
Logo	Placed in sidebar header, not top bar
Breadcrumb	Single-segment only (last path segment, humanized/capitalized)
Right-side content	"Agentic AI" status pill, org pill, user-menu trigger
"Agentic AI" pill:

bg-violet-50 border border-violet-100 text-violet-600 text-[9px] font-bold uppercase tracking-widest
with a 9px Brain icon. Only confirmed use of the Tailwind violet family in the app — a one-off accent.

Org pill: background: var(--brand-primary-light); color: var(--brand-primary) applied via inline style.

7. Button System
7.1 Shared base
.btn {
  @apply inline-flex items-center justify-center gap-2 px-4 py-2 text-[13px] font-semibold rounded-lg
         transition-all duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-1
         disabled:opacity-40 disabled:pointer-events-none select-none;
  letter-spacing: -0.01em;
}
Padding px-4 py-2, 13px semibold text, rounded-lg (8px), gap-2 icon spacing, transition-all 150ms, select-none.

7.2 Variants
Variant	Style
Primary	background: var(--brand-primary), white text, 1px solid var(--brand-primary) border, box-shadow: 0 2px 8px rgba(0,87,184,0.20). Hover → --brand-primary-dark. Active → scale(0.98)
Secondary	bg-white text-zinc-700 border border-zinc-200, hover bg-zinc-50 border-zinc-300, active scale(0.98)
Ghost	bg-transparent, text --color-text-muted, transparent border, hover bg-zinc-100 text-zinc-800
Danger	bg-white text-red-600 border border-red-100, hover bg-red-50 border-red-200
Success	No dedicated class — composed ad hoc where needed
Icon-only	No dedicated class — typically w-7 h-7 flex items-center justify-center rounded with hover bg/text shift
No explicit size variants (sm/md/lg) — one size for .btn; per-component overrides done inline as needed.

7.3 States
State	Visual treatment
Hover	Variant-specific bg/border/text shift
Active/pressed	transform: scale(0.98) — consistent across primary/secondary/logout button
Disabled	opacity-40, pointer-events-none
Focus	focus-visible:ring-2 focus-visible:ring-offset-1 (the only focus-visible: usage in the app)
Loading	No dedicated class — a spinning Loader2 icon (animate-spin) replaces/joins the label
7.4 Icons in Buttons
Lucide size prop, typically 13–15px, before or after label. Micro-interaction example: ArrowRight nudges via group-hover:translate-x-0.5 on hover.

7.5 Login-screen button variant (distinct from .btn)
Full-width primary CTA on the login screen uses a separate, hand-authored formula (not .btn-primary):

w-full h-11 text-white text-[14px] font-bold rounded-xl
background: var(--brand-primary) (or --brand-primary-dark while busy)
box-shadow: 0 4px 14px rgba(0,87,184,0.25)
Driven by framer-motion whileHover={{scale:1.01}} / whileTap={{scale:0.98}} instead of CSS :active. Taller (44px vs implicit .btn height), more rounded (rounded-xl/12px vs rounded-lg/8px), and a unique drop-shadow value not matching any global shadow token.

7.6 Ripple / Cursor
No ripple effect anywhere. No explicit cursor overrides beyond native button/disabled defaults (.input-professional does set cursor-not-allowed on disabled — see §8).

8. Form Components
8.1 Text Input — .input-professional
.input-professional {
  @apply flex h-9 w-full rounded-lg border bg-white px-3 py-2
         text-sm placeholder:text-zinc-400
         focus:outline-none focus:ring-2
         disabled:cursor-not-allowed disabled:opacity-50
         transition-all duration-150;
  border-color: var(--color-border-main);
  color: var(--color-text-main);
  letter-spacing: -0.005em;
}
.input-professional:focus {
  border-color: var(--brand-primary);
  box-shadow: 0 0 0 3px rgba(0,87,184,0.12);
}
Height 36px, padding px-3 py-2, border transitions to brand blue with a 3px glow on focus, placeholder text-zinc-400, disabled cursor-not-allowed opacity-50.

8.2 Password Input
Standard input styling plus an absolutely-positioned reveal-toggle icon button (Eye/EyeOff, 15px) inside the field, right-aligned.

8.3 Select / Dropdown
Global <select> base style (applies to every native select, no class needed):

select {
  appearance: none;
  background-image: url("... custom chevron-down SVG stroked in #5A6A7E ...");
  background-repeat: no-repeat;
  background-position: right 10px center;
  background-size: 14px;
  padding-right: 32px !important;
}
Replaces the native OS arrow with a custom inline-SVG chevron matching --color-text-muted.

8.4 Textarea, Multi-Select, Date Picker, File Upload, Checkbox/Radio/Switch
No dedicated global CSS classes or libraries confirmed for any of these. Requires manual verification of per-component ad hoc styling.

8.5 Validation / Error Styling
Inline error banner:

flex items-start gap-2.5 px-4 py-3 bg-red-50 border border-red-200 rounded-xl text-red-700 text-[13px] font-medium
with an AlertCircle icon (15px). Renders nothing when there's no message (no empty wrapper).

8.6 Disabled Styling
Inputs: cursor-not-allowed opacity-50. Buttons: opacity-40 pointer-events-none. (Differing opacity values between the two — a real, verified inconsistency.)

8.7 Focus Styling
Buttons: focus-visible:ring-2 focus-visible:ring-offset-1 (keyboard-only ring). Inputs: focus:ring-2 plus a custom border-color + box-shadow glow on any :focus (not :focus-visible) — an inconsistency between the two.

8.8 Placeholder Styling
placeholder:text-zinc-400 (default form inputs); placeholder:text-slate-400/placeholder:text-slate-200 on login-screen inputs (a slightly different gray scale used there — slate vs zinc).

8.9 Login-Screen Input Variant (distinct from .input-professional)
w-full h-11 bg-white border rounded-xl px-4 text-[14px] placeholder:text-slate-400
focus:outline-none transition-all
+ (error ? red-tinted border/ring : border-slate-200, focus:border-[#0057B8] focus:ring-2 focus:ring-[#0057B8]/10)
Diverges from .input-professional: height 44px (vs 36px), radius rounded-xl/12px (vs rounded-lg/8px), focus ring built from raw hex + Tailwind opacity suffix rather than the CSS-variable + box-shadow-glow approach used globally. A parallel, independently-authored input style specific to auth screens.

Large code-entry input (OTP-style, auth flows only):

w-full h-14 font-mono text-[22px] text-center tracking-[0.6em]
bg-white border border-slate-200 rounded-xl px-4
focus:border-[#0057B8] focus:ring-2 focus:ring-[#0057B8]/10
placeholder:text-slate-200
Tallest input in the app (56px), monospace, extreme letter-spacing to visually separate digits.

9. Tables
.table-clean { @apply w-full text-sm; border-collapse: collapse; color: var(--color-text-muted); }
.table-clean thead tr  { border-bottom: 1px solid var(--color-border-subtle); }
.table-clean thead th  { @apply px-4 py-3 text-left text-[11px] font-semibold uppercase tracking-wider; color: var(--color-text-dim); }
.table-clean tbody tr  { border-bottom: 1px solid var(--color-border-subtle); transition: background 0.1s; }
.table-clean tbody tr:hover { background: var(--brand-primary-light); }
.table-clean tbody td  { @apply px-4 py-3 text-[13px]; color: var(--color-text-muted); }
Feature	Spec
Header	11px semibold uppercase, tracking-wider, --color-text-dim, bottom border only, no fill
Rows	Bottom border only (no grid lines), background transitions 0.1s
Row hover	Background → --brand-primary-light
Cell padding	px-4 py-3
Cell text	13px, --color-text-muted
Sticky headers	Not implemented
Column resizing	Not implemented
Density	Single fixed density
Important: the app's primary data table (Findings list) does not use .table-clean/.badge-*/.sev-dot — it builds its own inline-Tailwind severity/status styling. The shared classes above ARE used elsewhere (repository list, credential manager). Two parallel badge/table visual systems coexist in production.

10. Cards
Property	.card-premium	.card-flat
Border	--color-border-subtle (lighter)	--color-border-main (stronger)
Radius	12px	12px
Shadow	--shadow-card, escalates to --shadow-premium on hover	0 1px 2px rgba(0,0,0,0.02), static
Stat card (KPI tile):

.stat-card  { @apply bg-white border rounded-xl p-5; border-color: var(--color-border-subtle); }
.stat-label { @apply text-[11px] font-semibold uppercase tracking-wider mb-1; color: var(--color-text-dim); }
.stat-value { @apply text-2xl font-semibold tracking-tight; color: var(--color-text-main); }
20px padding, 12px radius; an 11px uppercase label above a 24px semibold value.

11. Dashboard Widgets
11.1 Charts (Recharts)
Severity donut: PieChart/Pie/Cell, custom center label, clickable legend.
Trend area chart: two gradient-filled series (top-to-bottom linear gradient fade).
Project health bar chart: per-bar fill thresholded by score — >=80 emerald, >=60 amber, >=40 orange, else rose.
Shared tooltip: dark glassmorphic — bg-[#09090B] border-zinc-800, backdrop-blur-xl. The only dark-surface UI element confirmed anywhere in the (otherwise all-light) application.
Series colors: see §3.3(B) SEV_COLORDS.
11.2 Posture Gauge
Hand-rolled inline-SVG polar gauge, 240° sweep starting at −210°, arc-fill transition all 1s ease-out. Letter-grade (A–F) color map: each grade has its own {color, bg, border, text} set. Accompanying sparkline: raw <polyline> trend line with a directional trend icon (up/down/flat).

11.3 SLA / Overdue Widget
Hero count card with a 3-state color model: emerald (zero overdue), red (any critical overdue), amber (otherwise). Per-severity horizontal progress bars (transition-all duration-700). Aging-band breakdown displayed as stacked/grouped bars.

11.4 Progress Bars
Plain <div> width-percentage bars, transition-all duration-700 — no <progress> element or library used anywhere.

12. Navigation
12.1 Tabs
No generic reusable <Tabs> component. Tab-bar visual pattern (seen in the finding-detail modal): a row of labeled tab buttons with an active-state style distinguishing the selected tab (exact active-indicator styling: requires manual verification of full markup).

12.2 Dropdown Menus
Hand-built pattern (not a shared component), visually: a floating panel anchored below its trigger, white background, border, shadow, appearing on click (not hover).

12.3 Breadcrumbs
Single-segment only in the header (last path segment, humanized, capitalized) — not a multi-level trail.

12.4 Command Palette (the command palette component)
Overlay: bg-zinc-900/40 backdrop-blur-[2px]
Panel entrance: animate-in fade-in slide-in-from-top-2 duration-150
Highlighted result row: bg-[#E8F0FB] background, text-[#0057B8] text (hardcoded hex matching the brand-primary/brand-primary-light token values, rather than referencing the CSS variables directly)
Footer hint bar: small icon-labeled keyboard-shortcut legend
12.5 Context Menu
No custom right-click context menu found.

13. Icons
Property	Value
Library	lucide-react (exclusive — no other icon set)
Sizing	size={N} prop exclusively; never className="w-4 h-4"
Common sizes	9, 10, 11, 12, 13, 14, 15, 16, 20, 22, 36 (px)
Stroke widths	1.5 (large/decorative), 1.75 (sidebar nav — most standardized), 3 (small bold chevrons), default 2 elsewhere
Color rules	Generally inherit context text color; nav icons specifically: --color-text-dim at rest → --brand-primary on hover/active
Icon size generally scales with visual importance: hero/decorative icons ~20–36px, inline/label icons ~9–15px.

14. Typography
14.1 Font Family
'Inter', 'SF Pro Display', system-ui, -apple-system, sans-serif — Inter loaded via Google Fonts CDN (weights 400/500/600/700/800), no self-hosted font files, no @font-face rules. SF Pro Display is a fallback name only (not actually shipped).

14.2 Headings
Base rule: letter-spacing: -0.03em; font-weight: 600. No distinct per-level (h1 vs h4) size baked into global CSS — sizing applied per-usage via Tailwind.

14.3 Body
14px, line-height: 1.5, letter-spacing: -0.008em.

14.4 Caption / Micro-Label
Uppercase pattern: 10–11px, font-semibold/font-bold, uppercase, tracking-wider/tracking-widest, --color-text-dim.

14.5 Code / Monospace
react-syntax-highlighter used for code blocks (finding remediation diffs). Monospace also used for the login-screen OTP input and recovery-code chips.

14.6 Buttons
13px, font-semibold, letter-spacing: -0.01em (default .btn); login-screen buttons instead use 14px font-bold.

15. Color Usage Rules
Color	Where used
--brand-primary	Primary buttons, active nav, focus glow, links, login hero panel background
--brand-primary-dark	Primary button hover/active, submitting-state button background
--brand-primary-light	Nav/table row hover backgrounds, pill backgrounds, command-palette highlight
Red (red-*/--color-accent-red)	Logout button, danger buttons, critical severity, error banners
Amber (amber-*)	Medium severity, "sensitive but recoverable" actions (confirmed consistent semantic on the login/MFA screens specifically)
Orange (orange-*)	High severity
Emerald/Green (emerald-*/green-*)	Success states, healthy chart-bar threshold
Zinc (zinc-*)	Dominant neutral palette — text, borders, backgrounds, disabled states
Violet (violet-*)	Exactly one usage: header "Agentic AI" pill
Accent (--brand-accent)	Exactly one confirmed deliberate usage: login-screen headline accent word
16. Animation System
16.1 Keyframes
@keyframes fadeIn      { from { opacity: 0; }                              to { opacity: 1; } }
@keyframes zoomIn      { from { transform: scale(0.97); opacity: 0; }      to { transform: scale(1); opacity: 1; } }
@keyframes slideInUp   { from { transform: translateY(8px); opacity: 0; }  to { transform: translateY(0); opacity: 1; } }
@keyframes slideInLeft { from { transform: translateX(-8px); opacity: 0; } to { transform: translateX(0); opacity: 1; } }
slideInLeft has no corresponding utility class defined — likely dead CSS.

16.2 Defined Utility Classes
.animate-in   { animation-fill-mode: both; animation-timing-function: cubic-bezier(0.16,1,0.3,1); }
.fade-in      { animation-name: fadeIn;    animation-duration: 250ms; }
.zoom-in      { animation-name: zoomIn;    animation-duration: 200ms; }
.slide-in-up  { animation-name: slideInUp; animation-duration: 220ms; }
16.3 Undefined Classes In Use (flagged discrepancy)
slide-in-from-top-1/2, slide-in-from-bottom-4 appear in component class strings but are not defined anywhere and no plugin providing them is installed — likely inert. See §2.

16.4 Hover Animations
Card shadow escalation (.card-premium:hover, 0.2s ease)
Button bg/border/text color shifts (transition-all 150ms)
Nav icon/text color shift (0.15s ease)
Arrow-icon nudge: group-hover:translate-x-0.5
16.5 Press/Click Animations
transform: scale(0.98) on :active — consistent across primary/secondary buttons and the logout button. Login-screen buttons instead use framer-motion whileTap={{scale:0.98}} plus whileHover={{scale:1.01}} (an added hover-grow not present in the plain-CSS variant). One outlier: a Settings CTA button uses active:scale-95 (more pronounced than the 98% used elsewhere).

16.6 Loading Animation
animate-spin on Loader2 icons — the single most common animation class in the app, used for virtually every async/busy state. animate-pulse for skeleton loaders and pulsing status badges.

16.7 Skeleton Animation
Base: animate-pulse bg-zinc-200/80 rounded-lg, in four shape variants (text bar, circle, card block, button block).

16.8 Modal Animation
The generic modal component has no entrance/exit animation (appears/disappears instantly). Bespoke overlay implementations (e.g. command palette) DO animate in (fade-in slide-in-from-top-2 duration-150). A real inconsistency.

16.9 Toast Style
:root {
  --toastify-color-light: #ffffff;
  --toastify-text-color-light: #0D1B2E;
  --toastify-font-family: 'Inter', system-ui, sans-serif;
  --toastify-border-radius: 10px;
  --toastify-color-progress-light: var(--brand-primary);
}
.Toastify__toast {
  border: 1px solid var(--color-border-main) !important;
  box-shadow: var(--shadow-card) !important;
  font-size: 13px !important;
  padding: 12px 16px !important;
}
Position: bottom-right, auto-close 4000ms, light theme.

16.10 Duration Reference Table
Duration	Where
100ms	Table row hover background
150ms	Nav items, .btn, .input-professional, command-palette entrance
200ms	Fast dropdown/form-error entrance
220–250ms	Content fade/slide entrances
300–500ms	Card/page content entrance (most common pair)
700ms	Progress bar fills
1000ms	Posture gauge arc fill
16.11 Easing Reference
cubic-bezier(0.16, 1, 0.3, 1) is the app's one deliberate custom curve, applied both via the .animate-in CSS class and identically via framer-motion's ease prop on the login screen. Everything else relies on Tailwind's implicit default ease.

17. Responsive Design
17.1 Breakpoints
No custom breakpoints defined — Tailwind v3 defaults apply wherever used:

sm: 640px   md: 768px   lg: 1024px   xl: 1280px   2xl: 1536px
17.2 Observed Responsive Behavior
Settings panel: flex-col md:flex-row (stacks below 768px), grid-cols-1 md:grid-cols-3 (single column below 768px)
Login screen: left brand panel entirely hidden below lg (1024px); a smaller mobile-only logo substitutes in its place on the form panel
The main app shell (fixed 256px sidebar + fixed content margin) shows no confirmed mobile collapse behavior — no hamburger menu or off-canvas sidebar pattern found. The application appears primarily desktop-oriented, with only isolated components adapting per-breakpoint.
18. Login / Auth Screen
Covers the base login form (the login page's default render + the auth layout component). Multi-step MFA/password-reset sub-flows are out of scope (functional, not purely visual).

18.1 Overall Layout
Two-panel split, full viewport height:

<div className="min-h-screen flex w-full" style={{ background: '#F3F6FB' }}>
  <div>  left panel — brand, 44% width, hidden below lg (1024px)  </div>
  <div>  right panel — form, flex-1, centered  </div>
</div>
This screen does not use the .auth-page/.auth-card global CSS classes defined in index.css — it is a fully independent, hand-built visual implementation. (Whether any other page uses those legacy classes requires manual verification.)

18.2 Left Brand Panel (desktop only)
Property	Value
Width	44% of viewport
Background	var(--brand-primary)
Padding	56px (p-14)
Layout	Vertically split: logo top, headline middle, footer bottom
Decorative background layer (absolute, behind content):

500×500px circle, var(--brand-accent) at 10% opacity, bleeding off the top-right corner
320×320px white circle at 7% opacity, bottom-left
600×600px radial gradient (white at 4% opacity → transparent), centered
Logo block: white rounded card (rounded-xl, shadow-lg) containing the logo image, followed by an uppercase label (13px, semibold, light blue, tracking-wider).

Headline:

"Security" — 32px, font-black (900), white
"Operations Platform" — same size/weight, colored var(--brand-accent)
followed by a 14px medium-weight description in light blue (max-w-sm).

Footer row: copyright text (11px, light blue) on the left; a small green pulsing dot (animate-pulse) + "All systems operational" text (11px, light blue) on the right.

18.3 Right Form Panel
Centered, padding 32px/48px (responsive p-8 sm:p-12)
Content column max-width 420px
Entrance animation: fade + rise (opacity 0→1, y: 16px→0), 500ms, custom ease [0.16, 1, 0.3, 1]
Mobile-only logo substitute (shown only below lg/1024px, replacing the hidden left panel): smaller logo + uppercase blue label
Page title: 26px, font-black, brand blue; optional subtitle 14px muted; followed by a small decorative underline chip — 3px tall, 48px wide, rounded, filled brand blue
18.4 Form Fields
Both fields share one style formula (distinct from the app-wide .input-professional — see §8.9):

h-11 (44px) bg-white border rounded-xl (12px) px-4 text-[14px]
placeholder:text-slate-400
default border: border-slate-200, focus: border-[#0057B8] + ring-2 ring-[#0057B8]/10
error border: border-red-300, focus: border-red-400 + ring-2 ring-red-100
Email field: standard text input, red-tinted on validation error with an 11px red message below.
Password field: uses the shared password-input component (reveal-toggle eye icon, right-aligned inside the field).
A "Forgot password?" text link sits right-aligned above the password field (12px semibold, brand blue, underline on hover).
Field labels: 12px, semibold, uppercase, tracking-widest, muted color — consistent with the app's general uppercase micro-label convention.
18.5 Primary Submit Button
w-full h-11 rounded-xl text-white text-[14px] font-bold
background: var(--brand-primary) (var(--brand-primary-dark) while busy)
box-shadow: 0 4px 14px rgba(0,87,184,0.25)
hover: scale 1.01 / tap: scale 0.98 (motion-driven, not CSS :active)
A spinning loader icon replaces the label + arrow-icon while busy.

18.6 Inline Error/Status Banners
flex items-center gap-2.5 px-4 py-3 rounded-xl text-[13px] font-medium
error:   bg-red-50 border-red-200 text-red-700  (AlertCircle icon)
success: bg-green-50 border-green-200 text-green-700 (ShieldCheck icon)
Animated in with a fade + slight downward-to-rest motion.

18.7 Footer Text
Below the form: small (12px), medium-weight, muted gray, centered helper text.

18.8 Key Visual Divergences From the Rest of the App
Does not use .auth-page/.auth-card, .btn, or .input-professional — an entirely parallel, hand-authored style implementation for this screen only.
Larger control heights (44px inputs/buttons vs. 36px app-wide) and larger radius (rounded-xl/12px vs rounded-lg/8px).
--brand-accent gets its one clear, deliberate usage here (headline accent word) — elsewhere in the app the accent color is defined but not confirmed as used.
framer-motion scale-based hover/tap feedback here, vs. plain CSS :active{scale(0.98)} used by the rest of the app's buttons.
Focus-ring construction differs: raw hex + Tailwind opacity suffix (ring-[#0057B8]/10) here vs. CSS-variable + box-shadow glow (.input-professional) elsewhere.
19. Design Tokens Appendix
/* Brand */
--brand-primary:         #0057B8;
--brand-primary-dark:    #003E8A;
--brand-primary-light:   #E8F0FB;
--brand-accent:         #F5A300;
--brand-accent-light:   #FEF3CD;

/* Semantic aliases */
--color-primary:       var(--brand-primary);
--color-primary-hover: var(--brand-primary-dark);
--color-primary-light: var(--brand-primary-light);
--color-accent:        var(--brand-accent);
--color-accent-green:  #22c55e;
--color-accent-red:    #ef4444;
--color-accent-amber:  #f59e0b;

/* Surfaces */
--color-bg-main:  #F3F6FB;
--color-bg-card:  #ffffff;

/* Text */
--color-text-main:  #0D1B2E;
--color-text-muted: #5A6A7E;
--color-text-dim:   #94A3B8;

/* Borders */
--color-border-subtle: #EAF0F8;
--color-border-main:   #D5E0EE;

/* Layout dimensions */
--sidebar-width: 256px;
--topbar-height: 56px;

/* Shadows */
--shadow-card:    0 4px 24px rgba(0,87,184,0.06);
--shadow-premium: 0 8px 32px rgba(0,87,184,0.10);
--shadow-modal:   0 0 0 1px #D5E0EE, 0 16px 48px rgba(0,0,0,0.12);
// tailwind.config.js theme.extend
borderRadius: { card: '12px', btn: '8px' }
fontSize: { '2xs': ['10px', { lineHeight: '14px', letterSpacing: '0.04em' }] }
boxShadow: { card: 'var(--shadow-card)', premium: 'var(--shadow-premium)', modal: 'var(--shadow-modal)' }
Severity palette — pick ONE before reproducing (three divergent definitions coexist; see §3.3):

Set A: critical=red-600/red-50  high=orange-600/orange-50  medium=amber-700/amber-50  low=blue-600/blue-50  info=zinc-600/zinc-100
Set B: CRITICAL=#F43F5E  HIGH=#F59E0B  MEDIUM=#10B981  LOW=#3B82F6  INFO=#64748B
Set C: shape confirmed ({color,bg,text,border,icon}), exact values require manual verification
20. Migration Guide (Visual Reproduction Only)
20.1 Fonts
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
body { font-family: 'Inter', 'SF Pro Display', system-ui, -apple-system, sans-serif; }

20.2 Tailwind Config
Copy the theme.extend block from §19 verbatim (colors, shadows, radius, fontSize). Note the plugins: [] array is empty in source — if you want the slide-in-from-* classes seen in component code to actually resolve, you'd need to add tailwindcss-animate explicitly (source does not have it installed despite referencing its class names).

20.3 Global CSS Load Order
index.css declares @tailwind base/components/utilities directives, THEN the :root token block, in that order — reproduce in that sequence to match source exactly.

20.4 Icons
lucide-react, sized exclusively via the size prop (never Tailwind width/height classes), to match the exclusive convention found in source.

20.5 Dark Mode
Not implemented in source. Omit entirely for a faithful reproduction.

20.6 Priority Visual Elements to Port First
Global CSS classes: .btn*, .input-professional, .card-*, .badge-*, .sev-dot-*, .table-clean, .stat-*, .modal-*, sidebar/nav classes, then the login-screen's parallel style set (§18) as a separate "auth" style track.

End of document. Sections/items marked "Requires manual verification" were not confirmed against source and must not be treated as fact without further inspection.