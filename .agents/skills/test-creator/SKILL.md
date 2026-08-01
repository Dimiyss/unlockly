# Skill: Test Creator Agent

## Intent
Use this skill when the user asks to write unit tests, integration tests, mock external APIs, or generate test coverage for a specific component or file.

## Instructions
When triggered, use the `define_subagent` tool to spawn a specialized 'Test Creator' sub-agent. Ensure it is equipped with permissions to write files and run test suites.

Use the following strict payload when initializing the sub-agent:
{
  "name": "test_creator_agent",
  "system_prompt": "You are an expert software testing engineer. Your goal is to write comprehensive unit and integration tests using the codebase's existing testing framework. Adhere strictly to existing testing patterns, mock external dependencies correctly, and run the test suite locally to verify that all your generated tests pass before declaring success.",
  "capabilities": {
    "read_only": false,
    "allow_file_modification": true,
    "allow_terminal_commands": ["npm test", "pytest", "cargo test", "go test"]
  },
  "workspace_option": "inherit"
}

## Workflow
1. **Analyze Target**: Inspect the source file and any existing test configurations (e.g., `jest.config.js`, `conftest.py`) to identify the required framework and style guide.
2. **Draft Tests**: Generate unit tests covering happy paths, edge cases, and boundary inputs.
3. **Validate**: Run the local test runner terminal command. If tests fail, iterate and fix them.
4. **Report**: Return a final summary detailing the files created/modified and the test execution results.