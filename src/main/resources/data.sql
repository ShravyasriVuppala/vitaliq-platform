INSERT INTO exercises (id, name, description, category, primary_muscle_group, equipment_type, is_system_exercise, created_by_user_id, created_at, updated_at)
SELECT gen_random_uuid(), 'Bench Press', 'Barbell flat bench press targeting chest', 'WEIGHTED', 'CHEST', 'BARBELL', true, null, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM exercises WHERE name = 'Bench Press' AND is_system_exercise = true);

INSERT INTO exercises (id, name, description, category, primary_muscle_group, equipment_type, is_system_exercise, created_by_user_id, created_at, updated_at)
SELECT gen_random_uuid(), 'Squat', 'Barbell back squat targeting legs', 'WEIGHTED', 'LEGS', 'BARBELL', true, null, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM exercises WHERE name = 'Squat' AND is_system_exercise = true);

INSERT INTO exercises (id, name, description, category, primary_muscle_group, equipment_type, is_system_exercise, created_by_user_id, created_at, updated_at)
SELECT gen_random_uuid(), 'Deadlift', 'Conventional barbell deadlift targeting back', 'WEIGHTED', 'BACK', 'BARBELL', true, null, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM exercises WHERE name = 'Deadlift' AND is_system_exercise = true);

INSERT INTO exercises (id, name, description, category, primary_muscle_group, equipment_type, is_system_exercise, created_by_user_id, created_at, updated_at)
SELECT gen_random_uuid(), 'Overhead Press', 'Barbell overhead press targeting shoulders', 'WEIGHTED', 'SHOULDERS', 'BARBELL', true, null, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM exercises WHERE name = 'Overhead Press' AND is_system_exercise = true);

INSERT INTO exercises (id, name, description, category, primary_muscle_group, equipment_type, is_system_exercise, created_by_user_id, created_at, updated_at)
SELECT gen_random_uuid(), 'Pull Up', 'Bodyweight pull up targeting back', 'WEIGHTED', 'BACK', 'BODYWEIGHT', true, null, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM exercises WHERE name = 'Pull Up' AND is_system_exercise = true);

INSERT INTO exercises (id, name, description, category, primary_muscle_group, equipment_type, is_system_exercise, created_by_user_id, created_at, updated_at)
SELECT gen_random_uuid(), 'Push Up', 'Bodyweight push up targeting chest', 'WEIGHTED', 'CHEST', 'BODYWEIGHT', true, null, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM exercises WHERE name = 'Push Up' AND is_system_exercise = true);

INSERT INTO exercises (id, name, description, category, primary_muscle_group, equipment_type, is_system_exercise, created_by_user_id, created_at, updated_at)
SELECT gen_random_uuid(), 'Bicep Curl', 'Dumbbell bicep curl targeting biceps', 'WEIGHTED', 'BICEPS', 'DUMBBELL', true, null, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM exercises WHERE name = 'Bicep Curl' AND is_system_exercise = true);

INSERT INTO exercises (id, name, description, category, primary_muscle_group, equipment_type, is_system_exercise, created_by_user_id, created_at, updated_at)
SELECT gen_random_uuid(), 'Tricep Pushdown', 'Cable tricep pushdown targeting triceps', 'WEIGHTED', 'TRICEPS', 'CABLE', true, null, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM exercises WHERE name = 'Tricep Pushdown' AND is_system_exercise = true);

INSERT INTO exercises (id, name, description, category, primary_muscle_group, equipment_type, is_system_exercise, created_by_user_id, created_at, updated_at)
SELECT gen_random_uuid(), 'Running', 'Outdoor or treadmill running', 'CARDIO', 'CARDIO', null, true, null, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM exercises WHERE name = 'Running' AND is_system_exercise = true);

INSERT INTO exercises (id, name, description, category, primary_muscle_group, equipment_type, is_system_exercise, created_by_user_id, created_at, updated_at)
SELECT gen_random_uuid(), 'Plank', 'Core plank hold targeting core', 'WEIGHTED', 'CORE', 'BODYWEIGHT', true, null, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM exercises WHERE name = 'Plank' AND is_system_exercise = true);