import SwiftUI

/// The What's New sheet: a short, two-page release note in the platform's own
/// idiom (large title, SF Symbols, system text styles) with the app's coral
/// used only as an accent. Text uses Dynamic Type styles throughout, so each
/// page scrolls rather than clipping. The bottom button is pinned: it reads
/// "Continue" on the first page and "Done" on the last.
struct WhatsNewSheet: View {
    let release: WhatsNewRelease
    let scheme: ColorScheme

    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var page = 0
    @ScaledMetric(relativeTo: .body) private var badgeSize: CGFloat = 44

    private var pages: [[WhatsNewItem]] { release.pages }
    private var isLastPage: Bool { page >= pages.count - 1 }

    private var buttonTitle: LocalizedStringKey {
        isLastPage ? "whats_new.done" : "whats_new.continue"
    }

    var body: some View {
        VStack(spacing: 0) {
            TabView(selection: $page) {
                ForEach(Array(pages.enumerated()), id: \.offset) { index, items in
                    pageContent(index: index, items: items)
                        .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .animation(reduceMotion ? nil : .easeInOut(duration: 0.25), value: page)

            pageDots

            bottomButton
                .padding(.horizontal, 24)
                .padding(.top, 4)
                .padding(.bottom, 20)
                .background(Palette.paper(scheme))
        }
        .background(Palette.paper(scheme).ignoresSafeArea())
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        // Swiping down is a valid dismissal; the caller already marks the
        // version as shown when it presents the sheet.
    }

    private func pageContent(index: Int, items: [WhatsNewItem]) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 28) {
                if index == 0 {
                    firstHeader
                } else {
                    laterHeader
                }

                VStack(alignment: .leading, spacing: 24) {
                    ForEach(items) { item in
                        row(item)
                    }
                }
            }
            .frame(maxWidth: 560, alignment: .leading)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 24)
            .padding(.top, 8)
            .padding(.bottom, 24)
        }
    }

    private var firstHeader: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Spacer(minLength: 12)
                closeButton
            }

            Text("whats_new.title")
                .font(.largeTitle.bold())
                .foregroundStyle(Palette.ink(scheme))
                .accessibilityAddTraits(.isHeader)

            Text("whats_new.subtitle")
                .font(.body)
                .foregroundStyle(Palette.ink(scheme).opacity(0.65))
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var laterHeader: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                Spacer(minLength: 12)
                closeButton
            }

            Text("whats_new.page2_title")
                .font(.largeTitle.bold())
                .foregroundStyle(Palette.ink(scheme))
                .accessibilityAddTraits(.isHeader)
        }
    }

    private var pageDots: some View {
        HStack(spacing: 8) {
            ForEach(pages.indices, id: \.self) { index in
                Capsule(style: .continuous)
                    .fill(
                        index == page
                            ? Palette.coral(scheme)
                            : Palette.ink(scheme).opacity(0.2)
                    )
                    .frame(width: index == page ? 22 : 8, height: 8)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 8)
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.25), value: page)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text("Page \(page + 1) of \(pages.count)"))
    }

    private var closeButton: some View {
        Button {
            dismiss()
        } label: {
            Image(systemName: "xmark")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Palette.ink(scheme).opacity(0.65))
                .frame(width: 36, height: 36)
                .background(Palette.lift(scheme), in: Circle())
                .contentShape(Circle())
        }
        .buttonStyle(PressedButtonStyle())
        .accessibilityLabel(Text("common.done"))
    }

    private func row(_ item: WhatsNewItem) -> some View {
        HStack(alignment: .top, spacing: 14) {
            Image(systemName: item.systemImage)
                .font(.body.weight(.semibold))
                .foregroundStyle(Palette.coral(scheme))
                .frame(width: min(badgeSize, 60), height: min(badgeSize, 60))
                .background(
                    Palette.coral(scheme).opacity(0.12),
                    in: RoundedRectangle(cornerRadius: 12, style: .continuous)
                )
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 4) {
                Text(item.title)
                    .font(.headline)
                    .foregroundStyle(Palette.ink(scheme))
                Text(item.detail)
                    .font(.body)
                    .foregroundStyle(Palette.ink(scheme).opacity(0.7))
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(item.title))
        .accessibilityValue(Text(item.detail))
    }

    private var bottomButton: some View {
        Button {
            if isLastPage {
                dismiss()
            } else {
                withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.25)) {
                    page += 1
                }
            }
        } label: {
            Text(buttonTitle)
                .font(.headline)
                .foregroundStyle(Palette.onCoral(scheme))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 15)
                .background(
                    Palette.coral(scheme),
                    in: RoundedRectangle(cornerRadius: 14, style: .continuous)
                )
        }
        .buttonStyle(PressedButtonStyle())
    }
}
