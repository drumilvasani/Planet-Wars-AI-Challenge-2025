import time
from pathlib import Path

import requests
import subprocess
from typing import List
from runner_utils.process_issue import process_issue

POLL_INTERVAL = 60       # seconds
EVALUATION_TIMEOUT = 600  # seconds (10 minutes)
# Your GitHub repo and personal access token
REPO = "SimonLucas/planet-wars-rts-submissions"


def load_github_token() -> str:
    token_file = Path.home() / ".github_submission_token"
    if not token_file.exists():
        raise FileNotFoundError(f"GitHub token file not found at {token_file}")

    token = token_file.read_text().strip()
    return token


def get_open_issues(repo: str, github_token: str) -> List[dict]:
    url = f"https://api.github.com/repos/{repo}/issues"
    headers = {"Authorization": f"token {github_token}"}
    params = {"state": "open"}
    response = requests.get(url, headers=headers, params=params)
    response.raise_for_status()
    return response.json()

def add_label(repo: str, issue_number: int, labels: List[str], github_token: str) -> None:
    url = f"https://api.github.com/repos/{repo}/issues/{issue_number}/labels"
    headers = {
        "Authorization": f"token {github_token}",
        "Accept": "application/vnd.github+json",
    }
    data = {"labels": labels}
    response = requests.post(url, json=data, headers=headers)
    response.raise_for_status()

def poll_and_process(repo: str, github_token: str, base_dir: Path):
    print(f"🌀 Starting poller for {repo}")
    while True:
        try:
            issues = get_open_issues(repo, github_token)
            current_time = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())
            if not issues:
                # also print the current time for better visibility
                print(f"🔍 No open issues found at {current_time} Retrying... in {POLL_INTERVAL}s")
                time.sleep(POLL_INTERVAL)
                continue

            for issue in issues:
                issue_number = issue["number"]


                # Check if already being processed
                # if any(label["name"] == "processing" for label in issue["labels"]):
                #     continue

                print(f"⚙️ Processing issue #{issue_number}: {issue['title']} at {current_time}")

                # Mark as processing
                add_label(repo, issue_number, ["processing"], github_token)

                # Run evaluation with timeout
                try:
                    process_issue(issue, base_dir, github_token)
                except subprocess.TimeoutExpired:
                    print(f"⏰ Evaluation for issue #{issue_number} timed out after {EVALUATION_TIMEOUT}s")
                    # Optionally comment on the issue here
                except Exception as e:
                    print(f"❌ Error processing issue #{issue_number}: {e}")
                    # Optionally comment here too

        except Exception as e:
            print(f"🔴 Polling error: {e}")

        time.sleep(POLL_INTERVAL)

def main():
    base_dir = Path("/tmp/simonl-planetwars-run")
    token = load_github_token()
    poll_and_process(REPO, token, base_dir)

if __name__ == "__main__":
    main()