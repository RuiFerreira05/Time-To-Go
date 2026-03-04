---
trigger: always_on
description: This rule should be inforced when the agent determines that relevant information was determined/found
---

# Memory File Rules

- A `.memory` file located at `.agents\.memory` must be used as a shared knowledge base for all agents.
- Whenever an important decision, architectural choice, specific configuration, or key project context comes up that should be remembered across different sessions and by different agents, it MUST be documented in the `.memory` file.
- Before starting significant tasks, agents should review the `.memory` file to ensure they are aligned with the project's established conventions, ongoing issues, and historical decisions.
- Keep the `.memory` file concise but informative, organizing information logically so it can be easily parsed by other agents.