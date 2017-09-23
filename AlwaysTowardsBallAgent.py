import math
import numpy as np

'''
This is v2 code
'''

'''
Hi! You can use this code as a template to create your own bot.  Also if you don't mind writing a blurb
about your bot's strategy you can put it as a comment here. I'd appreciate it, especially if I can help
debug any runtime issues that occur with your bot.
'''

# Optional Information. Fill out only if you wish.

# Your real name:
# Contact Email:
# Can this bot's code be shared publicly (Default: No):
# Can non-tournment gameplay of this bot be displayed publicly (Default: No):


# This is the name that will be displayed on screen in the real time display!
BOT_NAME = "AlwaysTowardsBallAgent"


class agent:

	def __init__(self, team):
		self.team = team # use self.team to determine what team you are. I will set to "blue" or "orange"
		
	def convert_new_input_to_old_input(self, sharedValue):
	
		UU_TO_GAMEVALUES = 50
		
		UCONST_Pi = 3.1415926
		URotation180 = float(32768)
		URotationToRadians = UCONST_Pi / URotation180 
	
		inputs = np.zeros(38)
		scoring = np.zeros(12)
	
		gameTickPacket = sharedValue.GameTickPacket
		
		numCars = gameTickPacket.numCars
		numBoosts = gameTickPacket.numBoosts
		
		team1Blue = (gameTickPacket.gamecars[0].Team == 0)
		
		if team1Blue:
			blueIndex = 0
			orngIndex = 1
		else:
			blueIndex = 1
			orngIndex = 0
		
		# -------------------------------
		# First convert ball info
		# -------------------------------
		
		# Ball positions
		inputs[2] = gameTickPacket.gameball.Location.Y / UU_TO_GAMEVALUES
		inputs[7] = gameTickPacket.gameball.Location.X / UU_TO_GAMEVALUES
		inputs[17] = gameTickPacket.gameball.Location.Z / UU_TO_GAMEVALUES
		
		# Ball velocities
		inputs[28] = gameTickPacket.gameball.Velocity.X  / UU_TO_GAMEVALUES
		inputs[29] = gameTickPacket.gameball.Velocity.Z  / UU_TO_GAMEVALUES
		inputs[30] = gameTickPacket.gameball.Velocity.Y  / UU_TO_GAMEVALUES
		
		# -------------------------------
		# Now do all scoreboard values
		# -------------------------------
		scoring[0] = gameTickPacket.gamecars[blueIndex].Score.Goals + gameTickPacket.gamecars[1].Score.OwnGoals # Blue Scoreboard Score
		scoring[1] = gameTickPacket.gamecars[orngIndex].Score.Goals + gameTickPacket.gamecars[0].Score.OwnGoals # Orange Scoreboard Score
		scoring[2] = gameTickPacket.gamecars[orngIndex].Score.Demolitions # Demos by orange
		scoring[3] = gameTickPacket.gamecars[blueIndex].Score.Demolitions # Demos by blue
		scoring[4] = gameTickPacket.gamecars[blueIndex].Score.Score # Blue points
		scoring[5] = gameTickPacket.gamecars[orngIndex].Score.Score # Orange points
		scoring[6] = gameTickPacket.gamecars[blueIndex].Score.Goals # Blue Goals
		scoring[7] = gameTickPacket.gamecars[blueIndex].Score.Saves # Blue Saves
		scoring[8] = gameTickPacket.gamecars[blueIndex].Score.Shots # Blue Shots
		scoring[9] = gameTickPacket.gamecars[orngIndex].Score.Goals # Orange Goals
		scoring[10] = gameTickPacket.gamecars[orngIndex].Score.Saves # Orange Saves
		scoring[11] = gameTickPacket.gamecars[orngIndex].Score.Shots # Orange Shots
			
		# -------------------------------
		# Now do all car values
		# -------------------------------
		
		# Blue pos
		inputs[1] = gameTickPacket.gamecars[blueIndex].Location.Y / UU_TO_GAMEVALUES
		inputs[5] = gameTickPacket.gamecars[blueIndex].Location.X / UU_TO_GAMEVALUES
		inputs[4] = gameTickPacket.gamecars[blueIndex].Location.Z / UU_TO_GAMEVALUES
		
		# Orange pos
		inputs[3] = gameTickPacket.gamecars[orngIndex].Location.Y / UU_TO_GAMEVALUES
		inputs[18] = gameTickPacket.gamecars[orngIndex].Location.X / UU_TO_GAMEVALUES
		inputs[17] = gameTickPacket.gamecars[orngIndex].Location.Z / UU_TO_GAMEVALUES
		
		# Blue velocity
		inputs[28] = gameTickPacket.gamecars[blueIndex].Velocity.X / UU_TO_GAMEVALUES
		inputs[29] = gameTickPacket.gamecars[blueIndex].Velocity.Z / UU_TO_GAMEVALUES
		inputs[30] = gameTickPacket.gamecars[blueIndex].Velocity.Y / UU_TO_GAMEVALUES
		
		# Orange velocity
		inputs[34] = gameTickPacket.gamecars[orngIndex].Velocity.X / UU_TO_GAMEVALUES
		inputs[35] = gameTickPacket.gamecars[orngIndex].Velocity.Z / UU_TO_GAMEVALUES
		inputs[36] = gameTickPacket.gamecars[orngIndex].Velocity.Y / UU_TO_GAMEVALUES
		
		# Boost
		inputs[0] = gameTickPacket.gamecars[blueIndex].Boost
		inputs[37] = gameTickPacket.gamecars[orngIndex].Boost
		
		# Rotations
		bluePitch = float(gameTickPacket.gamecars[blueIndex].Rotation.Pitch)
		blueYaw = float(gameTickPacket.gamecars[blueIndex].Rotation.Yaw)
		blueRoll = float(gameTickPacket.gamecars[blueIndex].Rotation.Roll)
		orngPitch = float(gameTickPacket.gamecars[orngIndex].Rotation.Pitch)
		orngYaw = float(gameTickPacket.gamecars[orngIndex].Rotation.Yaw)
		orngRoll = float(gameTickPacket.gamecars[orngIndex].Rotation.Roll)
		
		# Blue rotations
		inputs[8] = math.cos(bluePitch * URotationToRadians) * math.cos(blueYaw * URotationToRadians) # Rot 1
		inputs[9] = math.sin(blueRoll * URotationToRadians) * math.sin(bluePitch * URotationToRadians) * math.cos(blueYaw * URotationToRadians) - math.cos(blueRoll * URotationToRadians) * math.sin(blueYaw * URotationToRadians) # Rot2
		inputs[10] = -1 * math.cos(blueRoll * URotationToRadians) * math.sin(bluePitch * URotationToRadians) * math.cos(blueYaw * URotationToRadians) + math.sin(blueRoll * URotationToRadians) * math.sin(blueYaw * URotationToRadians)  # Rot 3
		inputs[11] = math.cos(bluePitch * URotationToRadians) * math.sin(blueYaw * URotationToRadians) # Rot 4
		inputs[12] = math.sin(blueRoll * URotationToRadians) * math.sin(bluePitch * URotationToRadians) * math.sin(blueYaw * URotationToRadians) + math.cos(blueRoll * URotationToRadians) * math.cos(blueYaw * URotationToRadians) # Rot5
		inputs[13] = math.cos(blueYaw * URotationToRadians) * math.sin(blueRoll * URotationToRadians) - math.cos(blueRoll * URotationToRadians) * math.sin(bluePitch * URotationToRadians) * math.sin(blueYaw * URotationToRadians) # Rot 6
		inputs[14] = math.sin(bluePitch * URotationToRadians) # Rot 7
		inputs[15] = -1 * math.sin(blueRoll * URotationToRadians) * math.cos(bluePitch * URotationToRadians) # Rot 8
		inputs[16] = math.cos(blueRoll * URotationToRadians) * math.cos(bluePitch * URotationToRadians) # Rot 9
		
		# Orange rot
		inputs[19] = math.cos(orngPitch * URotationToRadians) * math.cos(orngYaw * URotationToRadians) # Rot 1
		inputs[20] = math.sin(orngRoll * URotationToRadians) * math.sin(orngPitch * URotationToRadians) * math.cos(orngYaw * URotationToRadians) - math.cos(orngRoll * URotationToRadians) * math.sin(orngYaw * URotationToRadians) # Rot2
		inputs[21] = -1 * math.cos(orngRoll * URotationToRadians) * math.sin(orngPitch * URotationToRadians) * math.cos(orngYaw * URotationToRadians) + math.sin(orngRoll * URotationToRadians) * math.sin(orngYaw * URotationToRadians)  # Rot 3
		inputs[22] = math.cos(orngPitch * URotationToRadians) * math.sin(orngYaw * URotationToRadians) # Rot 4
		inputs[23] = math.sin(orngRoll * URotationToRadians) * math.sin(orngPitch * URotationToRadians) * math.sin(orngYaw * URotationToRadians) + math.cos(orngRoll * URotationToRadians) * math.cos(orngYaw * URotationToRadians) # Rot5
		inputs[24] = math.cos(orngYaw * URotationToRadians) * math.sin(orngRoll * URotationToRadians) - math.cos(orngRoll * URotationToRadians) * math.sin(orngPitch * URotationToRadians) * math.sin(orngYaw * URotationToRadians) # Rot 6
		inputs[25] = math.sin(orngPitch * URotationToRadians) # Rot 7
		inputs[26] = -1 * math.sin(orngRoll * URotationToRadians) * math.cos(orngPitch * URotationToRadians) # Rot 8
		inputs[27] = math.cos(orngRoll * URotationToRadians) * math.cos(orngPitch * URotationToRadians) # Rot 9
		
		return(inputs,scoring)

	def get_output_vector(self, sharedValue):
	
		UCONST_Pi = 3.1415926
		URotation180 = float(32768)
		URotationToRadians = UCONST_Pi / URotation180 
	
		gameTickPacket = sharedValue.GameTickPacket
		
		team1Blue = (gameTickPacket.gamecars[0].Team == 0)
	
		if team1Blue:
			blueIndex = 0
			orngIndex = 1
		else:
			blueIndex = 1
			orngIndex = 0
		
		ball_y = gameTickPacket.gameball.Location.Y
		ball_x = gameTickPacket.gameball.Location.X
		bluePitch = float(gameTickPacket.gamecars[blueIndex].Rotation.Pitch)
		blueYaw = float(gameTickPacket.gamecars[blueIndex].Rotation.Yaw)
		orngPitch = float(gameTickPacket.gamecars[orngIndex].Rotation.Pitch)
		orngYaw = float(gameTickPacket.gamecars[orngIndex].Rotation.Yaw)
		
		turn = 16383

		if (self.team == "blue"):
			player_y = gameTickPacket.gamecars[blueIndex].Location.Y
			player_x = gameTickPacket.gamecars[blueIndex].Location.X
			player_rot1 = math.cos(bluePitch * URotationToRadians) * math.cos(blueYaw * URotationToRadians) # Rot 1
			player_rot4 = math.cos(bluePitch * URotationToRadians) * math.sin(blueYaw * URotationToRadians) # Rot 4
		else:
			player_y = gameTickPacket.gamecars[orngIndex].Location.Y
			player_x = gameTickPacket.gamecars[orngIndex].Location.X
			player_rot1 = math.cos(orngPitch * URotationToRadians) * math.cos(orngYaw * URotationToRadians) # Rot 1
			player_rot4 = math.cos(orngPitch * URotationToRadians) * math.sin(orngYaw * URotationToRadians) # Rot 4
		
		# Need to handle atan2(0,0) case, aka straight up or down, eventually
		player_front_direction_in_radians = math.atan2(player_rot1, player_rot4)
		relative_angle_to_ball_in_radians = math.atan2((ball_x - player_x), (ball_y - player_y))

		if (not (abs(player_front_direction_in_radians - relative_angle_to_ball_in_radians) < math.pi)):
			# Add 2pi to negative values
			if (player_front_direction_in_radians < 0):
				player_front_direction_in_radians += 2 * math.pi
			if (relative_angle_to_ball_in_radians < 0):
				relative_angle_to_ball_in_radians += 2 * math.pi

		if (relative_angle_to_ball_in_radians > player_front_direction_in_radians):
			turn = 0
		else:
			turn = 32767
		
		return [turn, 16383, 32767, 0, 0, 0, 0]
	