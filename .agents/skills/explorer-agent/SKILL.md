# Skill: Workspace Explorer Agent

## Intent
Use this skill when the user asks to analyze code layout, map architecture, find specific utility functions, or audit large portions of the codebase.

## Instructions
When triggered, use the `define_subagent` tool to spawn an asynchronous 'Explorer' clone. Task it with scanning the target directories, collecting relevant snippets, and returning a structured architectural summary.

### Tool Guidelines
- **Sub-agent Creation**: Use `define_subagent` to provision a dedicated, temporary workspace worker. 
- **Tool Constraints**: Explicitly restrict the sub-agent's allowed toolsets to read-only capabilities (like `view_file` or terminal search commands like `grep`, `find`) to keep the scan performant and safe.

## Workflow
1. **Scan Directories**: Recursively scan specified directories (defaulting to relevant source folders).
2. **Code Analysis**: Look for specific patterns, module interfaces, or architectural boundaries as requested.
3. **Report Generation**: Return a concise summary of findings.