package com.simibubi.create.foundation.render.backend.gl;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;

public class GlFog {
	public static float[] FOG_COLOR = new float[]{0, 0, 0, 0};

	public static boolean fogEnabled() {
		return GlStateManager.FOG.enable.enabled;
	}

	public static int getFogModeGlEnum() {
		return GlStateManager.FOG.mode;
	}

	public static float getFogDensity() {
		return GlStateManager.FOG.density;
	}

	public static float getFogEnd() {
		return GlStateManager.FOG.end;
	}

	public static float getFogStart() {
		return GlStateManager.FOG.start;
	}

	public static GlFogMode getFogMode() {
		if (!fogEnabled()) {
			return GlFogMode.NONE;
		}

		int mode = getFogModeGlEnum();

		switch (mode) {
			case GL11.GL_EXP2:
			case GL11.GL_EXP:
				return GlFogMode.EXP2;
			case GL11.GL_LINEAR:
				return GlFogMode.LINEAR;
			default:
				throw new UnsupportedOperationException("Unknown fog mode: " + mode);
		}
	}
}
