# Channel Permission and UI Enhancements

Update Channel functionality to restrict posting permissions (only creator by default, can allow admins) and improve UI with avatar support.

## User Review Required

> [!IMPORTANT]
> The current backend API doesn't seem to support `adminsCanPost` toggle. I will implement the client-side logic and models, and add a placeholder method in the repository.

## Proposed Changes

### Data Layer

#### [MODIFY] [Channel.kt](file:///home/pablito/StudioProjects/firstapp/app/src/main/java/com/example/firstapp/chat/model/Channel.kt)
- Add `adminsCanPost: Boolean = false` to the `Channel` data class.

#### [MODIFY] [ChannelEntity.kt](file:///home/pablito/StudioProjects/firstapp/app/src/main/java/com/example/firstapp/chat/data/local/entity/ChannelEntity.kt)
- Add `adminsCanPost: Boolean = false` to the `ChannelEntity` data class.

#### [MODIFY] [ChannelRepository.kt](file:///home/pablito/StudioProjects/firstapp/app/src/main/java/com/example/firstapp/chat/domain/repository/ChannelRepository.kt)
- Add `updateAdminPostingPermission(channelId: String, allowed: Boolean): Flow<Resource<Boolean>>`.

#### [MODIFY] [ChannelRepositoryImpl.kt](file:///home/pablito/StudioProjects/firstapp/app/src/main/java/com/example/firstapp/chat/data/repository/ChannelRepositoryImpl.kt)
- Implement `updateAdminPostingPermission` (local update and placeholder API call).
- Update mapping logic to include `adminsCanPost`.

### UI Layer

#### [MODIFY] [ChannelDetailViewModel.kt](file:///home/pablito/StudioProjects/firstapp/app/src/main/java/com/example/firstapp/chat/ui/viewmodel/ChannelDetailViewModel.kt)
- Expose `currentUserId` and `canPost` state.
- Add `toggleAdminPosting(allowed: Boolean)` method.

#### [MODIFY] [ChannelDetailScreen.kt](file:///home/pablito/StudioProjects/firstapp/app/src/main/java/com/example/firstapp/chat/ui/channels/ChannelDetailScreen.kt)
- Update `TopAppBar` to use `AsyncImage` for the avatar if `avatarUrl` is present.
- Restrict `FloatingActionButton` visibility: only show if the user is the creator OR (admins can post AND the user is an admin).
- Add "Allow Admins to Post" toggle in the `DropdownMenu` for the creator.

#### [MODIFY] [CreateChannelScreen.kt](file:///home/pablito/StudioProjects/firstapp/app/src/main/java/com/example/firstapp/chat/ui/channels/CreateChannelScreen.kt)
- Improve the avatar placeholder UI.

## Verification Plan

### Automated Tests
- Build the project to ensure no regressions in data mapping.

### Manual Verification
1.  Open a channel where you are the creator.
2.  Verify the "Create Post" FAB is visible.
3.  Toggle "Allow Admins to Post" in the menu.
4.  Verify the avatar is displayed correctly in the header.
5.  Take a screenshot of the `ChannelDetailScreen`.
