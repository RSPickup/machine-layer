# Git Development Workflow

## Project Setup
- Repository: RSPickup/machine-layer (Private)
- Main branch: `main` (default)

## Daily Workflow

### 1. Start New Task
```bash
# Always start from main
git checkout main
git pull origin main

# Create new task branch
git checkout -b task/task-name-or-id
```

### 2. Work on Task
```bash
# Make your changes
# Test your code

# Stage and commit changes
git add .
git commit -m "Task: Clear description of what was implemented"
```

### 3. Push and Create PR
```bash
# Push your task branch
git push origin task/task-name-or-id

# Go to GitHub and create Pull Request
```

## Branch Naming Convention
- `task/user-authentication`
- `task/database-setup`
- `task/api-endpoints`
- `fix/bug-description`

## Commit Message Format
- `Task: Implement user registration endpoint`
- `Feature: Add JWT authentication middleware`
- `Fix: Resolve database connection timeout`
- `Database: Design user and transaction schemas`

## Important Rules
1. **Never push directly to main** - always use feature branches
5. **Always pull main before creating new branch** - avoid conflicts

## Quick Commands
```bash
# Check current branch
git branch

# See all branches
git branch -a

# Check status
git status

# See commit history
git log --oneline

# See your contributions
git log --author="your-email" --oneline
```

## Emergency Commands
```bash
# If you made changes on wrong branch
git stash
git checkout correct-branch
git stash pop

# If you need to undo last commit (before push)
git reset HEAD~1

# If you need to see what changed
git diff
