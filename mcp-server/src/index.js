import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { ListToolsRequestSchema, CallToolRequestSchema } from "@modelcontextprotocol/sdk/types.js";
import axios from "axios";
import dotenv from "dotenv";

//import path from 'path';
//import { fileURLToPath } from 'url';
//
//const __filename = fileURLToPath(import.meta.url);
//const __dirname = path.dirname(__filename);
//
//// Load environment variables from .env file
//dotenv.config({ path: path.join(__dirname, '..', '.env') });

// Get API URL and key from environment
const API_URL = process.env.VITALIQ_API_URL || "http://localhost:8080";
const API_KEY = process.env.VITALIQ_API_KEY;

if (!API_KEY) {
  throw new Error("VITALIQ_API_KEY environment variable is not set");
}

console.error("🚀 VitalIQ MCP Server starting...");
console.error(`📡 API URL: ${API_URL}`);

// Create the MCP server
const server = new Server(
  {
    name: "vitaliq-mcp",
    version: "1.0.0",
  },
  {
    capabilities: {
      tools: {},
    },
  }
);


// Handle tool list requests from Claude
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return handleToolsList();
});

// Handle tool call requests from Claude
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  return handleToolCall(request.params);
});


// Define the tools that Claude can use
const TOOLS = [
  {
    name: "get_exercises",
    description: "Get list of available exercises with their IDs, categories, and equipment requirements",
    inputSchema: {
      type: "object",
      properties: {},
      required: []
    }
  },
  {
    name: "log_workout",
    description: "Log a completed workout with exercises, sets, and performance data",
    inputSchema: {
      type: "object",                       // Input is an object, object will have properties
      properties: {                         // Object has these fields:
        name: {
          type: "string",                   // Field type
          description: "Workout name (e.g., 'Chest and Triceps', 'Leg Day')"    // Field explanation
        },
        startTime: {
          type: "string",
          description: "ISO 8601 datetime when workout started (e.g., '2024-04-28T10:00:00')"
        },
        endTime: {
          type: "string",
          description: "ISO 8601 datetime when workout ended (e.g., '2024-04-28T11:00:00')"
        },
        exercises: {
          type: "array",
          description: "List of exercises performed in the workout",
          items: {
            type: "object",
            properties: {
              exerciseId: {
                type: "string",
                description: "UUID of the exercise (get from get_exercises tool)"
              },
              sets: {
                type: "array",              // Array field
                description: "List of sets for this exercise. Each set is either WEIGHTED or CARDIO",
                items: {                     // Each array item is:
                  type: "object",            // An object
                  oneOf: [                   // Each set is either: Weighted or Cardio
                    {
                      type: "object",
                      properties: {          // Option 1: WEIGHTED
                        type: {
                          type: "string",
                          enum: ["WEIGHTED"],
                          description: "Type of set"
                        },
                        reps: {
                          type: "number",
                          description: "Number of repetitions (required)"
                        },
                        weightLbs: {
                          type: "number",
                          description: "Weight used in lbs (optional for bodyweight)"
                        },
                        equipmentType: {
                          type: "string",
                          enum: ["DUMBBELL", "BARBELL", "MACHINE", "KETTLEBELL", "CABLE", "BODYWEIGHT"], // Allowed values
                          description: "Type of equipment used"
                        }
                      },
                      required: ["type", "reps", "equipmentType"]
                    },
                    {
                      type: "object",       // Option 2: CARDIO
                      properties: {
                        type: {
                          type: "string",
                          enum: ["CARDIO"],
                          description: "Type of set"
                        },
                        durationMinutes: {
                          type: "number",
                          description: "Duration of cardio in minutes (required)"
                        },
                        distanceMiles: {
                          type: "number",
                          description: "Distance covered in miles (optional)"
                        }
                      },
                      required: ["type", "durationMinutes"]
                    }
                  ]
                }
              },
              notes: {
                type: "string",
                description: "Optional notes about this exercise"
              }
            },
            required: ["exerciseId", "sets"]
          }
        },
        notes: {
          type: "string",
          description: "Optional overall notes about the workout"
        }
      },
      required: ["name", "startTime", "endTime", "exercises"]
    }
  },
  {
    name: "search_workouts",
    description: "Retrieve your workout history",
    inputSchema: {
      type: "object",
      properties: {},
      required: []
    }
  },
  {
    name: "get_dashboard",
    description: "Get your fitness dashboard with weekly and monthly statistics",
    inputSchema: {
      type: "object",
      properties: {},
      required: []
    }
  },
  {
    name: "generate_nutrition_plan",
    description: "Generate a personalized nutrition plan based on your profile and goals",
    inputSchema: {
      type: "object",
      properties: {
        planDate: {
          type: "string",
          description: "Date for the nutrition plan in YYYY-MM-DD format (optional, defaults to today)"
        }
      },
      required: []
    }
  },
  {
    name: "get_templates",
    description: "Get list of available workout templates (system and user-defined) with their exercises and IDs",
    inputSchema: {
      type: "object",
      properties: {},
      required: []
    }
  },
  {
    name: "log_workout_from_template",
    description: "Log a completed workout using a saved template with optional modifications. IMPORTANT: If you detect that the user has provided modifications to exercises: 1. Ask the user: 'Should I update the [TemplateName] template with these new values?' 2. Wait for their answer 3. Set updateTemplate=true if user says yes, false if they say no 4. Call this tool with the appropriate updateTemplate value If there are NO modifications, set updateTemplate=false directly (no ask needed).",
    inputSchema: {
      type: "object",
      properties: {
        templateId: {
          type: "string",
          description: "UUID of the template to log from (get from get_templates tool)"
        },
        startTime: {
          type: "string",
          description: "ISO 8601 datetime when workout started (optional, defaults to now)"
        },
        endTime: {
          type: "string",
          description: "ISO 8601 datetime when workout ended (optional, defaults to 1 hour after start)"
        },
        exerciseModifications: {
          type: "array",
          description: "Optional list of exercises to override with different sets than the template defaults",
          items: {
            type: "object",
            properties: {
              exerciseId: {
                type: "string",
                description: "UUID of the exercise to modify (get from get_exercises tool)"
              },
              modifiedSets: {
                type: "array",
                description: "Replacement sets for this exercise",
                items: {
                  type: "object",
                  oneOf: [
                    {
                      type: "object",
                      properties: {
                        type: { type: "string", enum: ["WEIGHTED"] },
                        reps: { type: "number", description: "Number of repetitions" },
                        weightLbs: { type: "number", description: "Weight in lbs (optional for bodyweight)" },
                        equipmentType: {
                          type: "string",
                          enum: ["DUMBBELL", "BARBELL", "MACHINE", "KETTLEBELL", "CABLE", "BODYWEIGHT"]
                        }
                      },
                      required: ["type", "reps", "equipmentType"]
                    },
                    {
                      type: "object",
                      properties: {
                        type: { type: "string", enum: ["CARDIO"] },
                        durationMinutes: { type: "number", description: "Duration in minutes" },
                        distanceMiles: { type: "number", description: "Distance in miles (optional)" }
                      },
                      required: ["type", "durationMinutes"]
                    }
                  ]
                }
              }
            },
            required: ["exerciseId", "modifiedSets"]
          }
        },
        notes: {
          type: "string",
          description: "Optional notes about the workout"
        },
        updateTemplate: {
          type: "boolean",
          description: "Whether to save the exercise modifications back to the template. Set true only when user explicitly confirms they want the template updated."
        }
      },
      required: ["templateId", "updateTemplate"]
    }
  },
  {
    name: "create_exercise",
    description: "Create a new user-defined exercise. Call get_exercises first to check if the exercise already exists before creating a duplicate.",
    inputSchema: {
      type: "object",
      properties: {
        name: {
          type: "string",
          description: "Exercise name (e.g., 'Incline Dumbbell Press')"
        },
        category: {
          type: "string",
          enum: ["WEIGHTED", "CARDIO"],
          description: "Exercise category"
        },
        primaryMuscleGroup: {
          type: "string",
          enum: ["CHEST", "BACK", "LEGS", "SHOULDERS", "BICEPS", "TRICEPS", "FOREARMS", "CORE", "GLUTES", "CARDIO"],
          description: "Primary muscle group targeted"
        },
        equipmentType: {
          type: "string",
          enum: ["DUMBBELL", "BARBELL", "MACHINE", "KETTLEBELL", "CABLE", "BODYWEIGHT"],
          description: "Equipment required for this exercise"
        }
      },
      required: ["name", "category", "primaryMuscleGroup", "equipmentType"]
    }
  },
  {
    name: "create_template",
    description: "Create a new workout template with exercises and sets. All exerciseIds must be valid UUIDs — use get_exercises or create_exercise to obtain them first.",
    inputSchema: {
      type: "object",
      properties: {
        name: {
          type: "string",
          description: "Template name (e.g., 'Chest Day', 'Pull Day')"
        },
        description: {
          type: "string",
          description: "Optional description of the template"
        },
        exercises: {
          type: "array",
          description: "Ordered list of exercises in the template",
          items: {
            type: "object",
            properties: {
              exerciseId: {
                type: "string",
                description: "UUID of the exercise (from get_exercises or create_exercise)"
              },
              orderIndex: {
                type: "number",
                description: "Position of this exercise in the template (0-based)"
              },
              sets: {
                type: "array",
                description: "Default sets for this exercise",
                items: {
                  type: "object",
                  oneOf: [
                    {
                      type: "object",
                      properties: {
                        type: { type: "string", enum: ["WEIGHTED"] },
                        reps: { type: "number", description: "Number of repetitions" },
                        weightLbs: { type: "number", description: "Weight in lbs (optional for bodyweight)" },
                        equipmentType: {
                          type: "string",
                          enum: ["DUMBBELL", "BARBELL", "MACHINE", "KETTLEBELL", "CABLE", "BODYWEIGHT"]
                        }
                      },
                      required: ["type", "reps", "equipmentType"]
                    },
                    {
                      type: "object",
                      properties: {
                        type: { type: "string", enum: ["CARDIO"] },
                        durationMinutes: { type: "number", description: "Duration in minutes" },
                        distanceMiles: { type: "number", description: "Distance in miles (optional)" }
                      },
                      required: ["type", "durationMinutes"]
                    }
                  ]
                }
              }
            },
            required: ["exerciseId", "orderIndex", "sets"]
          }
        }
      },
      required: ["name", "exercises"]
    }
  }
];

// ─────────────────────────────────────────────────────────────────────
// HANDLER: List available tools
// ─────────────────────────────────────────────────────────────────────

function handleToolsList() {
  return {
    tools: TOOLS
  };
}

// ─────────────────────────────────────────────────────────────────────
// HANDLER: Route tool calls to specific handlers
// ─────────────────────────────────────────────────────────────────────

async function handleToolCall(params) {
  const { name, arguments: toolInput } = params;

  console.error(`🔧 Executing tool: ${name}`);
  console.error(`📥 Input:`, JSON.stringify(toolInput, null, 2));

  try {
    let result;

    switch (name) {
      case "get_exercises":
        result = await handleGetExercises(toolInput);
        break;

      case "log_workout":
        result = await handleLogWorkout(toolInput);
        break;

      case "search_workouts":
        result = await handleSearchWorkouts(toolInput);
        break;

      case "get_dashboard":
        result = await handleGetDashboard(toolInput);
        break;

      case "generate_nutrition_plan":
        result = await handleGenerateNutritionPlan(toolInput);
        break;

      case "get_templates":
        result = await handleGetTemplates(toolInput);
        break;

      case "log_workout_from_template":
        result = await handleLogWorkoutFromTemplate(toolInput);
        break;

      case "create_exercise":
        result = await handleCreateExercise(toolInput);
        break;

      case "create_template":
        result = await handleCreateTemplate(toolInput);
        break;

      default:
        throw new Error(`Unknown tool: ${name}`);
    }

    console.error(`✅ Tool executed successfully`);
    return result;
  } catch (error) {
    console.error(`❌ Tool error: ${error.message}`);
    return {
      content: [
        {
          type: "text",
          text: `Error executing tool: ${error.message}`
        }
      ],
      isError: true
    };
  }
}

// ─────────────────────────────────────────────────────────────────────
// HANDLER IMPLEMENTATIONS
// ─────────────────────────────────────────────────────────────────────

async function handleGetExercises(input) {
  try {
    const response = await axios.get(
      `${API_URL}/api/exercises`,
      { headers: { "X-API-Key": API_KEY } }
    );

    return {
      content: [
        {
          type: "text",
          text: JSON.stringify(response.data, null, 2)
        }
      ]
    };
  } catch (error) {
    const detail = error.response?.data?.message ?? error.response?.data ?? error.message;
    return {
      content: [{ type: "text", text: `Error: ${detail}` }],
      isError: true
    };
  }
}

async function handleLogWorkout(input) {
  try {
    const { name, startTime, endTime, exercises, notes } = input;

    const requestBody = {
      name,
      startTime,
      endTime,
      exercises,
      notes
    };

    const response = await axios.post(
      `${API_URL}/api/workouts`,
      requestBody,
      { headers: { "X-API-Key": API_KEY } }
    );

    return {
      content: [
        {
          type: "text",
          text: `✅ Workout logged successfully!\n\n${JSON.stringify(response.data, null, 2)}`
        }
      ]
    };
  } catch (error) {
    const detail = error.response?.data?.message ?? error.response?.data ?? error.message;
    return {
      content: [{ type: "text", text: `Error logging workout: ${detail}` }],
      isError: true
    };
  }
}

async function handleSearchWorkouts(input) {
  try {
    const response = await axios.get(
      `${API_URL}/api/workouts`,
      { headers: { "X-API-Key": API_KEY } }
    );

    return {
      content: [
        {
          type: "text",
          text: JSON.stringify(response.data, null, 2)
        }
      ]
    };
  } catch (error) {
    const detail = error.response?.data?.message ?? error.response?.data ?? error.message;
    return {
      content: [{ type: "text", text: `Error: ${detail}` }],
      isError: true
    };
  }
}

async function handleGetDashboard(input) {
  try {
    const response = await axios.get(
      `${API_URL}/api/dashboard/summary`,
      { headers: { "X-API-Key": API_KEY } }
    );

    return {
      content: [
        {
          type: "text",
          text: JSON.stringify(response.data, null, 2)
        }
      ]
    };
  } catch (error) {
    const detail = error.response?.data?.message ?? error.response?.data ?? error.message;
    return {
      content: [{ type: "text", text: `Error: ${detail}` }],
      isError: true
    };
  }
}

async function handleGenerateNutritionPlan(input) {
  try {
    const { planDate } = input;

    const requestBody = planDate ? { planDate } : {};

    const response = await axios.post(
      `${API_URL}/api/nutrition/generate`,
      requestBody,
      { headers: { "X-API-Key": API_KEY } }
    );

    return {
      content: [
        {
          type: "text",
          text: `✅ Nutrition plan generated!\n\n${JSON.stringify(response.data, null, 2)}`
        }
      ]
    };
  } catch (error) {
    const detail = error.response?.data?.message ?? error.response?.data ?? error.message;
    return {
      content: [{ type: "text", text: `Error: ${detail}` }],
      isError: true
    };
  }
}


async function handleGetTemplates(input) {
  try {
    const response = await axios.get(
      `${API_URL}/api/templates`,
      { headers: { "X-API-Key": API_KEY } }
    );

    return {
      content: [
        {
          type: "text",
          text: JSON.stringify(response.data, null, 2)
        }
      ]
    };
  } catch (error) {
    const detail = error.response?.data?.message ?? error.response?.data ?? error.message;
    return {
      content: [{ type: "text", text: `Error: ${detail}` }],
      isError: true
    };
  }
}

async function handleLogWorkoutFromTemplate(input) {
  try {
    const { templateId, startTime, endTime, exerciseModifications, notes, updateTemplate } = input;

    const requestBody = {
      templateId,
      startTime,
      endTime,
      exerciseModifications,
      notes,
      updateTemplate
    };

    const response = await axios.post(
      `${API_URL}/api/workouts/from-template`,
      requestBody,
      { headers: { "X-API-Key": API_KEY } }
    );

    return {
      content: [
        {
          type: "text",
          text: `✅ Workout logged from template!\n\n${JSON.stringify(response.data, null, 2)}`
        }
      ]
    };
  } catch (error) {
    const detail = error.response?.data?.message ?? error.response?.data ?? error.message;
    return {
      content: [{ type: "text", text: `Error logging workout from template: ${detail}` }],
      isError: true
    };
  }
}


async function handleCreateExercise(input) {
  try {
    const { name, category, primaryMuscleGroup, equipmentType } = input;

    const response = await axios.post(
      `${API_URL}/api/exercises`,
      { name, category, primaryMuscleGroup, equipmentType },
      { headers: { "X-API-Key": API_KEY } }
    );

    return {
      content: [
        {
          type: "text",
          text: `✅ Exercise created!\n\nID: ${response.data.id}\nName: ${response.data.name}\nCategory: ${response.data.category}\nMuscle Group: ${response.data.primaryMuscleGroup}\nEquipment: ${response.data.equipmentType}`
        }
      ]
    };
  } catch (error) {
    const detail = error.response?.data?.message ?? error.response?.data ?? error.message;
    return {
      content: [{ type: "text", text: `Error creating exercise: ${detail}` }],
      isError: true
    };
  }
}

async function handleCreateTemplate(input) {
  try {
    const { name, description, exercises } = input;

    const response = await axios.post(
      `${API_URL}/api/templates`,
      { name, description: description ?? "", exercises },
      { headers: { "X-API-Key": API_KEY } }
    );

    return {
      content: [
        {
          type: "text",
          text: `✅ Template created!\n\nID: ${response.data.id}\nName: ${response.data.name}\nExercises: ${exercises.length}\n\n${JSON.stringify(response.data, null, 2)}`
        }
      ]
    };
  } catch (error) {
    const detail = error.response?.data?.message ?? error.response?.data ?? error.message;
    return {
      content: [{ type: "text", text: `Error creating template: ${detail}` }],
      isError: true
    };
  }
}


// ─────────────────────────────────────────────────────────────────────
// START THE SERVER
// ─────────────────────────────────────────────────────────────────────

async function main() {
  // Create stdio transport (communicates with Claude Desktop via stdin/stdout)
  const transport = new StdioServerTransport();

  // Connect transport to server
  await server.connect(transport);

  console.error("✅ VitalIQ MCP Server is running!");
  console.error(`🔐 Connected with API Key: configured`);
  console.error(`📡 Listening for Claude Desktop connections...\n`);
}

// Run the server
main().catch((error) => {
  console.error("❌ Server error:", error);
  process.exit(1);
});
