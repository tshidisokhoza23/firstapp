import os
import re

viewmodels = [
    "/home/tshidiso/Desktop/firstapp/app/src/main/java/com/example/firstapp/home/ui/viewmodel/SearchViewModel.kt",
    "/home/tshidiso/Desktop/firstapp/app/src/main/java/com/example/firstapp/home/ui/viewmodel/MainViewModel.kt",
    "/home/tshidiso/Desktop/firstapp/app/src/main/java/com/example/firstapp/chat/ui/viewmodel/ChatViewModel.kt",
    "/home/tshidiso/Desktop/firstapp/app/src/main/java/com/example/firstapp/media/ui/viewmodel/ShortsViewModel.kt",
    "/home/tshidiso/Desktop/firstapp/app/src/main/java/com/example/firstapp/auth/ui/viewmodel/AuthViewModel.kt",
    "/home/tshidiso/Desktop/firstapp/app/src/main/java/com/example/firstapp/music/ui/viewmodel/MusicViewModel.kt",
    "/home/tshidiso/Desktop/firstapp/app/src/main/java/com/example/firstapp/common/ui/viewmodel/UserViewModel.kt"
]

for path in viewmodels:
    if not os.path.exists(path): continue
    with open(path, 'r') as f:
        content = f.read()
    
    # Add imports
    content = re.sub(r'package (.*)', r'package \1\n\nimport dagger.hilt.android.lifecycle.HiltViewModel\nimport javax.inject.Inject', content, count=1)
    
    # Add @HiltViewModel and @Inject constructor
    content = re.sub(r'class (\w+ViewModel)\s*\(\)\s*: ViewModel\(\)', r'@HiltViewModel\nclass \1 @Inject constructor() : ViewModel()', content)
    content = re.sub(r'class (\w+ViewModel)\s*:\s*ViewModel\(\)', r'@HiltViewModel\nclass \1 @Inject constructor() : ViewModel()', content)

    # Convert AppContainer calls to constructor injection (simplified)
    # This is complex to do via regex for all cases, but we can try common ones.
    
    with open(path, 'w') as f:
        f.write(content)
