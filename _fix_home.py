import sys
path = r'C:\Users\fpc20\Desktop\aiMall\mall-v1\mobile-h5\src\views\Home.vue'
with open(path, encoding='utf-8') as f:
    content = f.read()

# Add click to search bar
content = content.replace(
    '<div class="search-bar">',
    '<div class="search-bar" @click="$router.push(\'/search\')">'
)

# Add click to nav items
content = content.replace(
    'v-for="(item, i) in navItems"',
    'v-for="(item, i) in navItems" @click="$router.push(\'/product/\' + (i+1))"'
)

# Add click to flash items
content = content.replace(
    'v-for="(item, i) in flashItems" :key="i"',
    'v-for="(item, i) in flashItems" :key="i" @click="$router.push(\'/product/\' + (i+1))"'
)

# Add click to recommend products
content = content.replace(
    'v-for="(item, i) in recommendList"',
    'v-for="(item, i) in recommendList" @click="$router.push(\'/product/\' + (i+1))"'
)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print('OK')