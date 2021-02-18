/*******************************************************************************
 * Copyright (c) 2021 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.desktop.functions;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionUsage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;

import static com.blackrook.rookscript.lang.ScriptFunctionUsage.type;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;

/**
 * Script functions for image manipulation.
 * @author Matthew Tropiano
 */
public enum ImageFunctions implements ScriptFunctionType
{
	IMAGE(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a new, blank image of the specified width and height (32-bit ARGB format)."
				)
				.parameter("width", 
					type(Type.INTEGER, "Width in pixels.")
				)
				.parameter("height", 
					type(Type.INTEGER, "Height in pixels.")
				)
				.returns(
					type(Type.OBJECTREF, "BufferedImage", "The new image."),
					type(Type.ERROR, "BadDimensions", "If width or height is 0 or less.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				int height = temp.asInt();
				scriptInstance.popStackValue(temp);
				int width = temp.asInt();
				
				if (width < 1)
				{
					returnValue.setError("BadDimensions", "Width is less than 1.");
					return true;
				}
				else if (height < 1)
				{
					returnValue.setError("BadDimensions", "Height is less than 1.");
					return true;
				}
				
				returnValue.set(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	IMAGEREAD(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a new image by reading it from a file or stream. " +
					"The data must be in a format recognized by Java ImageIO."
				)
				.parameter("source", 
					type(Type.STRING, "The path to a file."),
					type(Type.OBJECTREF, "File", "The path to the file."),
					type(Type.OBJECTREF, "InputStream", "The open stream to read."),
					type(Type.OBJECTREF, "URL", "A URL to an image to read.")
				)
				.returns(
					type(Type.OBJECTREF, "Image", "The image loaded from the source."),
					type(Type.ERROR, "BadImage", "If the data could not be interpreted."),
					type(Type.ERROR, "IOError", "If the stream could not be read.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				if (temp.isNull())
				{
					returnValue.setError("BadImage", "Source is null.");
					return true;
				}
				
				BufferedImage image;
				if (temp.isObjectType(File.class))
				{
					try {
						image = ImageIO.read(temp.asObjectType(File.class));
					} catch (IOException e) {
						returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
						return true;
					}
				}
				else if (temp.isObjectType(InputStream.class))
				{
					try {
						image = ImageIO.read(temp.asObjectType(InputStream.class));
					} catch (IOException e) {
						returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
						return true;
					}
				}
				else if (temp.isObjectType(URL.class))
				{
					try {
						image = ImageIO.read(temp.asObjectType(URL.class));
					} catch (IOException e) {
						returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
						return true;
					}
				}
				else
				{
					try {
						image = ImageIO.read(new File(temp.asString()));
					} catch (IOException e) {
						returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
						return true;
					}
				}
				
				// Test for integer raster. If not, convert (the composites rely on it).
				if (image.getData().getSampleModel().getDataType() != DataBuffer.TYPE_INT)
				{
					BufferedImage converted = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = converted.createGraphics();
					g.drawImage(image, 0, 0, null);
					g.dispose();
					image = converted;
				}
				
				returnValue.set(image);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	IMAGEWRITE(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Writes an image out to a file or stream. " +
					"The target data type must be in a format recognized by Java ImageIO."
				)
				.parameter("image", 
					type(Type.OBJECTREF, "BufferedImage", "The image to write.")
				)
				.parameter("destination", 
					type(Type.STRING, "The path to a file."),
					type(Type.OBJECTREF, "File", "The path to a target file."),
					type(Type.OBJECTREF, "OutputStream", "The open stream to write to.")
				)
				.parameter("type",
					type(Type.NULL, "Detect from file extension."),
					type(Type.STRING, "The informal type name.")
				)
				.returns(
					type(Type.NULL, "If the destination is null."),
					type(Type.STRING, "The file path, if [destination] is a string."),
					type(Type.OBJECTREF, "File", "The file, if [destination] is a File."),
					type(Type.OBJECTREF, "OutputStream", "The stream, if [destination] is an OutputStream."),
					type(Type.ERROR, "BadImage", "If the first parameter is not a BufferedImage."),
					type(Type.ERROR, "BadFormat", "If the [type] is not a valid type, nor inferred by output."),
					type(Type.ERROR, "IOError", "If the stream could not be written to.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			ScriptValue dest = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(temp);
				String type = temp.isNull() ? null : temp.asString();
				scriptInstance.popStackValue(dest);
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectType(BufferedImage.class))
				{
					returnValue.setError("BadImage", "First parameter is not an image.");
					return true;
				}
				BufferedImage image = temp.asObjectType(BufferedImage.class);
				
				if (dest.isNull())
				{
					returnValue.setNull();
					return true;
				}
				else if (dest.isObjectType(File.class))
				{
					int x = 0;
					File file = dest.asObjectType(File.class);
					String fileName = file.getPath();
					if (type == null)
					{
						if ((x = fileName.lastIndexOf('.')) < 0)
						{
							returnValue.setError("BadFormat", "Image was not written - no format provided or inferrable.");
							return true;
						}
						else
						{
							type = fileName.substring(x + 1);
						}
					}
					
					try {
						if (!ImageIO.write(image, type, file))
							returnValue.setError("BadFormat", "Image was not written - no appropriate writer for format: " + type);
						else
							returnValue.set(file);
					} catch (IOException e) {
						returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					}
					return true;
				}
				else if (dest.isObjectType(OutputStream.class))
				{
					if (type == null)
					{
						returnValue.setError("BadFormat", "Image was not written - no format provided.");
						return true;
					}
					
					try {
						OutputStream out = temp.asObjectType(OutputStream.class);
						if (!ImageIO.write(image, type, out))
							returnValue.setError("BadFormat", "Image was not written - no appropriate writer for format: " + type);
						else
							returnValue.set(out);
					} catch (IOException e) {
						returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					}
					return true;
				}
				else
				{
					int x = 0;
					String fileName = dest.asString();
					if (type == null)
					{
						if ((x = fileName.lastIndexOf('.')) < 0)
						{
							returnValue.setError("BadFormat", "Image was not written - no format provided or inferrable.");
							return true;
						}
						else
						{
							type = fileName.substring(x + 1);
						}
					}
					
					try {
						File file = new File(fileName);
						if (!ImageIO.write(image, type, file))
							returnValue.setError("BadFormat", "Image was not written - no appropriate writer for format: " + type);
						else
							returnValue.set(fileName);
					} catch (IOException e) {
						returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					}
					return true;
				}
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	IMAGECOPY(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a new image from an existing image, such that the new image is a copy of the provided image."
				)
				.parameter("image", 
					type(Type.OBJECTREF, "BufferedImage", "The image to copy.")
				)
				.returns(
					type(Type.OBJECTREF, "BufferedImage", "A new image that is a copy of [image]."),
					type(Type.ERROR, "BadImage", "If the first parameter is not a BufferedImage.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectType(BufferedImage.class))
				{
					returnValue.setError("BadImage", "First parameter is not an image.");
					return true;
				}
				
				BufferedImage image = temp.asObjectType(BufferedImage.class);
				ColorModel cm = image.getColorModel();
			    boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
			    WritableRaster raster = image.copyData(image.getRaster().createCompatibleWritableRaster());
				returnValue.set(new BufferedImage(cm, raster, isAlphaPremultiplied, null));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	IMAGERESIZE(4)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a new image that is a resized version of a source image."
				)
				.parameter("image", 
					type(Type.OBJECTREF, "BufferedImage", "The image to inspect.")
				)
				.parameter("width", 
					type(Type.INTEGER, "New width in pixels.")
				)
				.parameter("height", 
					type(Type.INTEGER, "New height in pixels.")
				)
				.parameter("mode", 
					type(Type.NULL, "Use Nearest Neighbor ('nearest')."),
					type(Type.STRING, "A resize mode: 'nearest', 'linear', 'bilinear', 'bicubic'.")
				)
				.returns(
					type(Type.OBJECTREF, "BufferedImage", "A new image that is [image] with the resized dimensions."),
					type(Type.ERROR, "BadImage", "If the first parameter is not a BufferedImage."),
					type(Type.ERROR, "BadMode", "If the provided [mode] is an unsupported mode."),
					type(Type.ERROR, "BadDimensions", "If width or height is 0 or less.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				ResamplingType mode = temp.isNull() ? ResamplingType.NEAREST : ResamplingType.VALUES.get(temp.asString());
				scriptInstance.popStackValue(temp);
				int height = temp.asInt();
				scriptInstance.popStackValue(temp);
				int width = temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectType(BufferedImage.class))
				{
					returnValue.setError("BadImage", "First parameter is not an image.");
					return true;
				}
				if (mode == null)
				{
					returnValue.setError("BadMode", "Resize mode is unsupported. Must be: 'nearest', 'linear', 'bilinear', 'bicubic'");
					return true;
				}
				if (width < 1)
				{
					returnValue.setError("BadDimensions", "Width is less than 1.");
					return true;
				}
				if (height < 1)
				{
					returnValue.setError("BadDimensions", "Height is less than 1.");
					return true;
				}
				
				BufferedImage image = temp.asObjectType(BufferedImage.class);
				BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = out.createGraphics();
				mode.setHints(g);
				g.drawImage(image, 0, 0, width, height, null);
				g.dispose();
				
				returnValue.set(out);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	}, 
	
	IMAGESCALE(4)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a new image that is a resized version of a source image using scalar values."
				)
				.parameter("image", 
					type(Type.OBJECTREF, "BufferedImage", "The image to inspect.")
				)
				.parameter("scaleX", 
					type(Type.INTEGER, "Scalar value, X-axis.")
				)
				.parameter("scaleY", 
					type(Type.INTEGER, "Scalar value, Y-axis.")
				)
				.parameter("mode", 
					type(Type.NULL, "Use Nearest Neighbor ('nearest')."),
					type(Type.STRING, "A resize mode: 'nearest', 'linear', 'bilinear', 'bicubic'.")
				)
				.returns(
					type(Type.OBJECTREF, "BufferedImage", "A new image that is [image] with the scaled dimensions."),
					type(Type.ERROR, "BadImage", "If the first parameter is not a BufferedImage."),
					type(Type.ERROR, "BadMode", "If the fourth parameter is an unsupported mode."),
					type(Type.ERROR, "BadDimensions", "If width or height will be 0 or less.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				ResamplingType mode = temp.isNull() ? ResamplingType.NEAREST : ResamplingType.VALUES.get(temp.asString());
				scriptInstance.popStackValue(temp);
				double scaleY = temp.asDouble();
				scriptInstance.popStackValue(temp);
				double scaleX = temp.asDouble();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectType(BufferedImage.class))
				{
					returnValue.setError("BadImage", "First parameter is not an image.");
					return true;
				}
				if (mode == null)
				{
					returnValue.setError("BadMode", "Resize mode is unsupported. Must be: 'nearest', 'linear', 'bilinear', 'bicubic'");
					return true;
				}
				
				BufferedImage image = temp.asObjectType(BufferedImage.class);				
				
				int width = (int)(scaleX * image.getWidth());
				int height = (int)(scaleY * image.getHeight());
				
				if (width < 1)
				{
					returnValue.setError("BadDimensions", "Width is less than 1.");
					return true;
				}
				if (height < 1)
				{
					returnValue.setError("BadDimensions", "Height is less than 1.");
					return true;
				}
				
				BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = out.createGraphics();
				mode.setHints(g);
				g.drawImage(image, 0, 0, width, height, null);
				g.dispose();
				
				returnValue.set(out);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	}, 
	
	IMAGECROP(5)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a new image that is a cropped area of a source image."
				)
				.parameter("image", 
					type(Type.OBJECTREF, "BufferedImage", "The image to inspect.")
				)
				.parameter("x", 
					type(Type.INTEGER, "X-axis offset in pixels (from left side of image).")
				)
				.parameter("y", 
					type(Type.INTEGER, "Y-axis offset in pixels (from top of image).")
				)
				.parameter("width", 
					type(Type.INTEGER, "Width of crop area in pixels.")
				)
				.parameter("height", 
					type(Type.INTEGER, "Height of crop area in pixels.")
				)
				.returns(
					type(Type.OBJECTREF, "BufferedImage", "A new image that is [image] with the cropped dimensions."),
					type(Type.ERROR, "BadImage", "If the first parameter is not a BufferedImage."),
					type(Type.ERROR, "BadDimensions", "If width or height is 0 or less.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				int height = temp.asInt();
				scriptInstance.popStackValue(temp);
				int width = temp.asInt();
				scriptInstance.popStackValue(temp);
				int y = temp.asInt();
				scriptInstance.popStackValue(temp);
				int x = temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectType(BufferedImage.class))
				{
					returnValue.setError("BadImage", "First parameter is not an image.");
					return true;
				}
				if (width < 1)
				{
					returnValue.setError("BadDimensions", "Width is less than 1 or not specified.");
					return true;
				}
				if (height < 1)
				{
					returnValue.setError("BadDimensions", "Height is less than 1 or not specified.");
					return true;
				}
				
				BufferedImage image = temp.asObjectType(BufferedImage.class);
				BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = out.createGraphics();
				g.drawImage(image, -x, -y, image.getWidth(), image.getHeight(), null);
				g.dispose();
				
				returnValue.set(out);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	}, 
	
	IMAGEFLIPH(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a new image by flipping a source image horizontally."
				)
				.parameter("image", 
					type(Type.OBJECTREF, "BufferedImage", "The image to flip.")
				)
				.returns(
					type(Type.OBJECTREF, "BufferedImage", "A new image that is [image], flipped horizontally."),
					type(Type.ERROR, "BadImage", "If the first parameter is not a BufferedImage.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectType(BufferedImage.class))
				{
					returnValue.setError("BadImage", "First parameter is not an image.");
					return true;
				}
				
				BufferedImage image = temp.asObjectType(BufferedImage.class);
				BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = out.createGraphics();
				g.drawImage(image, image.getWidth(), 0, -image.getWidth(), image.getHeight(), null);
				g.dispose();
				
				returnValue.set(out);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	}, 
	
	IMAGEFLIPV(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a new image by flipping a source image vertically."
				)
				.parameter("image", 
					type(Type.OBJECTREF, "BufferedImage", "The image to flip.")
				)
				.returns(
					type(Type.OBJECTREF, "BufferedImage", "A new image that is [image], flipped vertically."),
					type(Type.ERROR, "BadImage", "If the first parameter is not a BufferedImage.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectType(BufferedImage.class))
				{
					returnValue.setError("BadImage", "First parameter is not an image.");
					return true;
				}
				
				BufferedImage image = temp.asObjectType(BufferedImage.class);
				BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = out.createGraphics();
				g.drawImage(image, 0, image.getHeight(), image.getWidth(), -image.getHeight(), null);
				g.dispose();
				
				returnValue.set(out);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	}, 
	
	IMAGETRANSPOSE(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a new image by transposing a source image's X and Y axes."
				)
				.parameter("image", 
					type(Type.OBJECTREF, "BufferedImage", "The image to transpose.")
				)
				.returns(
					type(Type.OBJECTREF, "BufferedImage", "A new image that is [image], transposed."),
					type(Type.ERROR, "BadImage", "If the first parameter is not a BufferedImage.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectType(BufferedImage.class))
				{
					returnValue.setError("BadImage", "First parameter is not an image.");
					return true;
				}
				
				BufferedImage image = temp.asObjectType(BufferedImage.class);
				BufferedImage out = new BufferedImage(image.getHeight(), image.getWidth(), BufferedImage.TYPE_INT_ARGB);
				for (int x = 0; x < image.getWidth(); x++)
					for (int y = 0; y < image.getHeight(); y++)
						out.setRGB(y, x, image.getRGB(x, y));
				
				returnValue.set(out);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	}, 
	
	IMAGEFILL(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Fills an image with the provided color."
				)
				.parameter("image", 
					type(Type.OBJECTREF, "BufferedImage", "The image to fill.")
				)
				.parameter("color", 
					type(Type.INTEGER, "The color, formatted as a 32-bit ARGB value.")
				)
				.returns(
					type(Type.OBJECTREF, "BufferedImage", "[image]"),
					type(Type.ERROR, "BadImage", "If the first parameter is not a BufferedImage.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				int color = temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectType(BufferedImage.class))
				{
					returnValue.setError("BadImage", "First parameter is not an image.");
					return true;
				}
				
				BufferedImage image = temp.asObjectType(BufferedImage.class);
				Graphics2D g = image.createGraphics();
				g.setColor(new Color(color, true));
				g.fillRect(0, 0, image.getWidth(), image.getHeight());
				g.dispose();
				
				returnValue.set(image);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	}, 
	
	IMAGEPAINT(9)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Paints an image into another image, changing the target image."
				)
				.parameter("image", 
					type(Type.OBJECTREF, "BufferedImage", "The image to paint into.")
				)
				.parameter("sourceImage", 
					type(Type.OBJECTREF, "BufferedImage", "The image to paint.")
				)
				.parameter("x", 
					type(Type.NULL, "Use 0."),
					type(Type.INTEGER, "X-axis offset in pixels (from left side of image).")
				)
				.parameter("y", 
					type(Type.NULL, "Use 0."),
					type(Type.INTEGER, "Y-axis offset in pixels (from top of image).")
				)
				.parameter("width", 
					type(Type.NULL, "Use source image width."),
					type(Type.INTEGER, "Width in pixels.")
				)
				.parameter("height", 
					type(Type.NULL, "Use source image height."),
					type(Type.INTEGER, "Height in pixels.")
				)
				.parameter("blend", 
					type(Type.NULL, "Use alpha compositing ('alpha')."),
					type(Type.STRING, "A blending mode: 'replace', 'alpha', 'add', 'subtract', 'multiply', 'desaturate'.")
				)
				.parameter("prealpha", 
					type(Type.NULL, "Use 1.0."),
					type(Type.FLOAT, "The pre-alpha scalar (image alpha is also applied). NOTE: 'replace' does not use this value.")
				)
				.parameter("mode", 
					type(Type.NULL, "Use Nearest Neighbor ('nearest')."),
					type(Type.STRING, "A resize mode: 'nearest', 'linear', 'bilinear', 'bicubic'.")
				)
				.returns(
					type(Type.OBJECTREF, "BufferedImage", "[image]"),
					type(Type.ERROR, "BadImage", "If the first or second parameter is not a BufferedImage."),
					type(Type.ERROR, "BadMode", "If the provided [mode] is an unsupported mode."),
					type(Type.ERROR, "BadBlend", "If the provided [blend] is not a valid blend."),
					type(Type.ERROR, "BadDimensions", "If width or height is 0 or less.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			ScriptValue temp2 = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(temp);
				ResamplingType mode = temp.isNull() ? ResamplingType.NEAREST : ResamplingType.VALUES.get(temp.asString());
				scriptInstance.popStackValue(temp);
				float alpha = temp.isNull() ? 1.0f : temp.asFloat();
				scriptInstance.popStackValue(temp);
				CompositingTypes blend = temp.isNull() ? CompositingTypes.ALPHA : CompositingTypes.VALUES.get(temp.asString());
				scriptInstance.popStackValue(temp);
				Integer height = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				Integer width = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				int y = temp.asInt();
				scriptInstance.popStackValue(temp);
				int x = temp.asInt();
				scriptInstance.popStackValue(temp2);
				scriptInstance.popStackValue(temp);
				
				if (!temp.isObjectType(BufferedImage.class))
				{
					returnValue.setError("BadImage", "First parameter is not an image.");
					return true;
				}
				if (!temp2.isObjectType(BufferedImage.class))
				{
					returnValue.setError("BadImage", "Second parameter is not an image.");
					return true;
				}
				if (mode == null)
				{
					returnValue.setError("BadMode", "Resize mode is unsupported. Must be: 'nearest', 'linear', 'bilinear', 'bicubic'");
					return true;
				}
				if (blend == null)
				{
					returnValue.setError("BadBlend", "Blend type is unsupported. Must be: 'replace', 'alpha', 'add', 'subtract', 'multiply', 'desaturate'.");
					return true;
				}
				
				BufferedImage image = temp.asObjectType(BufferedImage.class);
				BufferedImage sourceImage = temp2.asObjectType(BufferedImage.class);
				if (width == null)
					width = sourceImage.getWidth();
				if (height == null)
					height = sourceImage.getHeight();
				
				if (width < 1)
				{
					returnValue.setError("BadDimensions", "Width is less than 1.");
					return true;
				}
				if (height < 1)
				{
					returnValue.setError("BadDimensions", "Height is less than 1.");
					return true;
				}

				Graphics2D g = image.createGraphics();
				Composite oldComposite = blend.setComposite(g, alpha);
				mode.setHints(g);
				g.drawImage(sourceImage, x, y, width, height, null);
				g.setComposite(oldComposite);
				g.dispose();
				
				returnValue.set(image);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	}, 
	
	/** @since 1.10.2.1 */
	IMAGEDRAW(6)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Paints an image into another image, changing the target image. " +
					"To contrast with IMAGEPAINT(), this is a convenience function that does not accept resizing parameters."
				)
				.parameter("image", 
					type(Type.OBJECTREF, "BufferedImage", "The image to paint into.")
				)
				.parameter("sourceImage", 
					type(Type.OBJECTREF, "BufferedImage", "The image to paint.")
				)
				.parameter("x", 
					type(Type.NULL, "Use 0."),
					type(Type.INTEGER, "X-axis offset in pixels (from left side of image).")
				)
				.parameter("y", 
					type(Type.NULL, "Use 0."),
					type(Type.INTEGER, "Y-axis offset in pixels (from top of image).")
				)
				.parameter("blend", 
					type(Type.NULL, "Use alpha compositing ('alpha')."),
					type(Type.STRING, "A blending mode: 'replace', 'alpha', 'add', 'subtract', 'multiply', 'desaturate'.")
				)
				.parameter("prealpha", 
					type(Type.NULL, "Use 1.0."),
					type(Type.FLOAT, "The pre-alpha scalar (image alpha is also applied). NOTE: 'replace' does not use this value.")
				)
				.returns(
					type(Type.OBJECTREF, "BufferedImage", "[image]"),
					type(Type.ERROR, "BadImage", "If the first or second parameter is not a BufferedImage."),
					type(Type.ERROR, "BadBlend", "If the provided [blend] is not a valid blend."),
					type(Type.ERROR, "BadDimensions", "If width or height is 0 or less.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			ScriptValue temp2 = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(temp);
				float alpha = temp.isNull() ? 1.0f : temp.asFloat();
				scriptInstance.popStackValue(temp);
				CompositingTypes blend = temp.isNull() ? CompositingTypes.ALPHA : CompositingTypes.VALUES.get(temp.asString());
				scriptInstance.popStackValue(temp);
				Integer height = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				Integer width = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				int y = temp.asInt();
				scriptInstance.popStackValue(temp);
				int x = temp.asInt();
				scriptInstance.popStackValue(temp2);
				scriptInstance.popStackValue(temp);
				
				if (!temp.isObjectType(BufferedImage.class))
				{
					returnValue.setError("BadImage", "First parameter is not an image.");
					return true;
				}
				if (!temp2.isObjectType(BufferedImage.class))
				{
					returnValue.setError("BadImage", "Second parameter is not an image.");
					return true;
				}
				if (blend == null)
				{
					returnValue.setError("BadBlend", "Blend type is unsupported. Must be: 'replace', 'alpha', 'add', 'subtract', 'multiply', 'desaturate'.");
					return true;
				}
				
				BufferedImage image = temp.asObjectType(BufferedImage.class);
				BufferedImage sourceImage = temp2.asObjectType(BufferedImage.class);
				if (width == null)
					width = sourceImage.getWidth();
				if (height == null)
					height = sourceImage.getHeight();
				
				if (width < 1)
				{
					returnValue.setError("BadDimensions", "Width is less than 1.");
					return true;
				}
				if (height < 1)
				{
					returnValue.setError("BadDimensions", "Height is less than 1.");
					return true;
				}

				Graphics2D g = image.createGraphics();
				Composite oldComposite = blend.setComposite(g, alpha);
				g.drawImage(sourceImage, x, y, width, height, null);
				g.setComposite(oldComposite);
				g.dispose();
				
				returnValue.set(image);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	}, 
	
	IMAGESET(4)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Sets an image's pixel data."
				)
				.parameter("image", 
					type(Type.OBJECTREF, "BufferedImage", "The image to use.")
				)
				.parameter("x", 
					type(Type.NULL, "Use 0."),
					type(Type.INTEGER, "Image X-coordinate (from left side of image).")
				)
				.parameter("y", 
					type(Type.NULL, "Use 0."),
					type(Type.INTEGER, "Image Y-coordinate (from top of image).")
				)
				.parameter("color", 
					type(Type.INTEGER, "The color, formatted as a 32-bit ARGB value.")
				)
				.returns(
					type(Type.OBJECTREF, "BufferedImage", "[image]."),
					type(Type.ERROR, "BadImage", "If the first parameter is not a BufferedImage."),
					type(Type.ERROR, "BadCoordinates", "If X or Y is out of bounds.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				int color = temp.asInt();
				scriptInstance.popStackValue(temp);
				int y = temp.asInt();
				scriptInstance.popStackValue(temp);
				int x = temp.asInt();
				scriptInstance.popStackValue(temp);
				
				if (!temp.isObjectType(BufferedImage.class))
				{
					returnValue.setError("BadImage", "First parameter is not an image.");
					return true;
				}
				BufferedImage image = temp.asObjectType(BufferedImage.class);
				if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight())
				{
					returnValue.setError("BadCoordinates", "Coordinates are outside image bounds.");
					return true;
				}
				
				image.setRGB(x, y, color);
				returnValue.set(image);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	IMAGEGET(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets an image's pixel data."
				)
				.parameter("image", 
					type(Type.OBJECTREF, "BufferedImage", "The image to use.")
				)
				.parameter("x", 
					type(Type.NULL, "Use 0."),
					type(Type.INTEGER, "Image X-coordinate (from left side of image).")
				)
				.parameter("y", 
					type(Type.NULL, "Use 0."),
					type(Type.INTEGER, "Image Y-coordinate (from top of image).")
				)
				.returns(
					type(Type.INTEGER, "The image pixel data, 32-bit ARGB."),
					type(Type.ERROR, "BadImage", "If the first parameter is not a BufferedImage."),
					type(Type.ERROR, "BadCoordinates", "If X or Y is out of bounds.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				int y = temp.asInt();
				scriptInstance.popStackValue(temp);
				int x = temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectType(BufferedImage.class))
				{
					returnValue.setError("BadImage", "First parameter is not an image.");
					return true;
				}
				returnValue.set(temp.asObjectType(BufferedImage.class).getRGB(x, y));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	IMAGEWIDTH(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets the width of an image in pixels."
				)
				.parameter("image", 
					type(Type.OBJECTREF, "BufferedImage", "The image to inspect.")
				)
				.returns(
					type(Type.INTEGER, "The image width."),
					type(Type.ERROR, "BadImage", "If the first parameter is not a BufferedImage.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectType(BufferedImage.class))
				{
					returnValue.setError("BadImage", "First parameter is not an image.");
					return true;
				}
				returnValue.set(temp.asObjectType(BufferedImage.class).getWidth());
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	IMAGEHEIGHT(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets the height of an image in pixels."
				)
				.parameter("image", 
					type(Type.OBJECTREF, "BufferedImage", "The image to inspect.")
				)
				.returns(
					type(Type.INTEGER, "The image height."),
					type(Type.ERROR, "BadImage", "If the first parameter is not a BufferedImage.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectType(BufferedImage.class))
				{
					returnValue.setError("BadImage", "First parameter is not an image.");
					return true;
				}
				returnValue.set(temp.asObjectType(BufferedImage.class).getHeight());
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	;
	
	private final int parameterCount;
	private Usage usage;
	private ImageFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(ImageFunctions.values());
	}

	@Override
	public int getParameterCount()
	{
		return parameterCount;
	}

	@Override
	public Usage getUsage()
	{
		if (usage == null)
			usage = usage();
		return usage;
	}
	
	@Override
	public abstract boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue);

	protected abstract Usage usage();

	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE2 = ThreadLocal.withInitial(()->ScriptValue.create(null));

	// =======================================================================
	// =======================================================================

	/**
	 * Resampling types.
	 */
	public enum ResamplingType
	{
		NEAREST
		{
			@Override
			public void setHints(Graphics2D g)
			{
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
				g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			}
		},
		
		LINEAR
		{
			@Override
			public void setHints(Graphics2D g)
			{
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			}
		},
		
		BILINEAR
		{
			@Override
			public void setHints(Graphics2D g)
			{
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			}
		},
		
		BICUBIC
		{
			@Override
			public void setHints(Graphics2D g)
			{
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			}
		},
		;
		
		
		/**
		 * Sets the rendering hints for this type.
		 * @param g the graphics context.
		 */
		public abstract void setHints(Graphics2D g);
		
		public static final Map<String, ResamplingType> VALUES = new TreeMap<String, ResamplingType>(String.CASE_INSENSITIVE_ORDER)
		{
			private static final long serialVersionUID = -6575715699170949164L;
			{
				for (ResamplingType type : ResamplingType.values())
				{
					put(type.name(), type);
				}
			}
		};
	}
	
	/**
	 * Compositing types.
	 */
	public enum CompositingTypes
	{
		REPLACE
		{
			@Override
			public Composite setComposite(Graphics2D g, float scalar)
			{
				Composite old = g.getComposite();
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
				return old;
			}
		},
		
		ALPHA
		{
			@Override
			public Composite setComposite(Graphics2D g, float scalar)
			{
				Composite old = g.getComposite();
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, scalar));
				return old;
			}
		},
		
		ADD
		{
			@Override
			public Composite setComposite(Graphics2D g, float scalar)
			{
				Composite old = g.getComposite();
				g.setComposite(AdditiveComposite.getInstance(scalar));
				return old;
			}
		},
		
		SUBTRACT
		{
			@Override
			public Composite setComposite(Graphics2D g, float scalar)
			{
				Composite old = g.getComposite();
				g.setComposite(SubtractiveComposite.getInstance(scalar));
				return old;
			}
		},
		
		MULTIPLY
		{
			@Override
			public Composite setComposite(Graphics2D g, float scalar)
			{
				Composite old = g.getComposite();
				g.setComposite(MultiplicativeComposite.getInstance(scalar));
				return old;
			}
		},
		
		/** @since 1.10.2.1 */
		DESATURATE
		{
			@Override
			public Composite setComposite(Graphics2D g, float scalar)
			{
				Composite old = g.getComposite();
				g.setComposite(DesaturationComposite.getInstance(scalar));
				return old;
			}
		},
		
		;
		
		/**
		 * Push the composite for this type.
		 * @param g the graphics context.
		 * @param scalar the applicative scalar value.
		 * @return the old composite.
		 */
		public abstract Composite setComposite(Graphics2D g, float scalar);

		public static final Map<String, CompositingTypes> VALUES = new TreeMap<String, CompositingTypes>(String.CASE_INSENSITIVE_ORDER)
		{
			private static final long serialVersionUID = 907874275883556484L;
			{
				for (CompositingTypes type : CompositingTypes.values())
				{
					put(type.name(), type);
				}
			}
		};
	}
	
	/**
	 * A composite that adds pixel color together.
	 * The scalar amount for the addition per pixel is taken from the alpha component.  
	 */
	public static final class AdditiveComposite implements Composite
	{
		private static final AdditiveComposite INSTANCE = new AdditiveComposite();
		
		private float scalar;
		
		private AdditiveComposite()
		{
			this.scalar = 1f;
		}

		private AdditiveComposite(float scalar)
		{
			this.scalar = scalar;
		}

		/**
		 * @return an instance of this composite.
		 */
		public static AdditiveComposite getInstance()
		{
			return INSTANCE;
		}
		
		/**
		 * @param scalar the applicative scalar.
		 * @return an instance of this composite.
		 */
		public static AdditiveComposite getInstance(float scalar)
		{
			return new AdditiveComposite(scalar);
		}
		
		@Override
		public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) 
		{
			return new AdditiveCompositeContext(srcColorModel, dstColorModel, scalar);
		}
		
	}
	
	/**
	 * A composite that subtracts pixel color.
	 * The scalar amount for the subtraction per pixel is taken from the alpha component.  
	 */
	public static final class SubtractiveComposite implements Composite
	{
		private static final SubtractiveComposite INSTANCE = new SubtractiveComposite();
		
		private float scalar;
		
		private SubtractiveComposite()
		{
			this.scalar = 1f;
		}

		private SubtractiveComposite(float scalar)
		{
			this.scalar = scalar;
		}

		/**
		 * @return an instance of this composite.
		 */
		public static SubtractiveComposite getInstance()
		{
			return INSTANCE;
		}
		
		/**
		 * @param scalar the applicative scalar.
		 * @return an instance of this composite.
		 */
		public static SubtractiveComposite getInstance(float scalar)
		{
			return new SubtractiveComposite(scalar);
		}
		
		@Override
		public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) 
		{
			return new SubtractiveCompositeContext(srcColorModel, dstColorModel, scalar);
		}
		
	}
	
	/**
	 * A composite that multiplies pixel color together.
	 * The scalar amount for the multiply per pixel is taken from the alpha component.  
	 */
	public static final class MultiplicativeComposite implements Composite
	{
		private static final MultiplicativeComposite INSTANCE = new MultiplicativeComposite();
		
		private float scalar;

		private MultiplicativeComposite()
		{
			this.scalar = 1f;
		}

		private MultiplicativeComposite(float scalar)
		{
			this.scalar = scalar;
		}

		/**
		 * @return an instance of this composite.
		 */
		public static MultiplicativeComposite getInstance()
		{
			return INSTANCE;
		}
		
		/**
		 * @param scalar the applicative scalar.
		 * @return an instance of this composite.
		 */
		public static MultiplicativeComposite getInstance(float scalar)
		{
			return new MultiplicativeComposite(scalar);
		}
		
		@Override
		public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) 
		{
			return new MultiplicativeCompositeContext(srcColorModel, dstColorModel, scalar);
		}
		
	}
	
	/**
	 * A composite that multiplies pixel color together.
	 * The scalar amount for the multiply per pixel is taken from the alpha component.  
	 * @since 1.10.2.1
	 */
	public static final class DesaturationComposite implements Composite
	{
		private static final DesaturationComposite INSTANCE = new DesaturationComposite();
		
		private float scalar;

		private DesaturationComposite()
		{
			this.scalar = 1f;
		}

		private DesaturationComposite(float scalar)
		{
			this.scalar = scalar;
		}

		/**
		 * @return an instance of this composite.
		 */
		public static DesaturationComposite getInstance()
		{
			return INSTANCE;
		}
		
		/**
		 * @param scalar the applicative scalar.
		 * @return an instance of this composite.
		 */
		public static DesaturationComposite getInstance(float scalar)
		{
			return new DesaturationComposite(scalar);
		}
		
		@Override
		public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) 
		{
			return new DesaturationCompositeContext(srcColorModel, dstColorModel, scalar);
		}
		
	}
	
	// =======================================================================
	// =======================================================================

	/**
	 * All composite contexts that mix two pixels together.
	 */
	private static abstract class ARGBCompositeContext implements CompositeContext
	{
		protected ColorModel srcColorModel; 
		protected ColorModel dstColorModel;
		protected int preAlpha;
		
		/**
		 * Creates a new context with the provided color models and hints.
		 * @param srcColorModel the color model of the source.
		 * @param dstColorModel the color model of the destination.
		 * @param preAlpha the alpha to pre-apply (0 to 1).
		 */
		protected ARGBCompositeContext(ColorModel srcColorModel, ColorModel dstColorModel, float preAlpha)
		{
			this.srcColorModel = srcColorModel;
			this.dstColorModel = dstColorModel;
			this.preAlpha = (int)(preAlpha * 255);
		}
		
		/**
		 * Checks if a {@link Raster} is the correct data format for this compositing operation.
		 * @param colorModel the color model to check compatibility for.
		 * @param raster the Raster to check.
		 * @throws UnsupportedOperationException if the Raster's data type is not {@link DataBuffer#TYPE_INT}.
		 */
		protected static void checkRaster(ColorModel colorModel, Raster raster) 
		{
	        if (!colorModel.isCompatibleRaster(raster))
	            throw new UnsupportedOperationException("ColorModel is not compatible with raster.");
	        if (raster.getSampleModel().getDataType() != DataBuffer.TYPE_INT)
	            throw new UnsupportedOperationException("Expected integer data type from raster.");
	    }
		
		/**
		 * Mixes two pixels together.
		 * @param srcARGB the incoming ARGB 32-bit integer value.
		 * @param dstARGB the existing, "source" ARGB 32-bit integer value.
		 * @return the resultant ARGB value.
		 */
		protected abstract int composePixel(int srcARGB, int dstARGB);
		
		@Override
		public void compose(Raster src, Raster dstIn, WritableRaster dstOut)
		{
			// alpha of 0 = do nothing.
			if (preAlpha == 0)
				return;
			
			checkRaster(srcColorModel, src);
			checkRaster(dstColorModel, dstIn);
			checkRaster(dstColorModel, dstOut);
			
			int width = Math.min(src.getWidth(), dstIn.getWidth());
			int height = Math.min(src.getHeight(), dstIn.getHeight());
			int[] srcRowBuffer = new int[width];
			int[] dstRowBuffer = new int[width];
			
			for (int y = 0; y < height; y++) 
			{
				src.getDataElements(0, y, width, 1, srcRowBuffer);
				dstIn.getDataElements(0, y, width, 1, dstRowBuffer);
				
				for (int x = 0; x < width; x++)
					dstRowBuffer[x] = composePixel(srcColorModel.getRGB(srcRowBuffer[x]), dstColorModel.getRGB(dstRowBuffer[x]));
				
				dstOut.setDataElements(0, y, width, 1, dstRowBuffer);
			}
		}

		@Override
		public void dispose() 
		{
			this.srcColorModel = null;
			this.dstColorModel = null;
		}
	}
	
	/**
	 * The composite context for {@link AdditiveComposite}s. 
	 */
	public static class AdditiveCompositeContext extends ARGBCompositeContext
	{
		private AdditiveCompositeContext(ColorModel srcColorModel, ColorModel dstColorModel, float preAlpha)
		{
			super(srcColorModel, dstColorModel, preAlpha);
		}

		@Override
		protected int composePixel(int srcARGB, int dstARGB) 
		{
			int srcBlue =  (srcARGB & 0x000000FF);
			int dstBlue =  (dstARGB & 0x000000FF);
			int srcGreen = (srcARGB & 0x0000FF00) >>> 8;
			int dstGreen = (dstARGB & 0x0000FF00) >>> 8;
			int srcRed =   (srcARGB & 0x00FF0000) >>> 16;
			int dstRed =   (dstARGB & 0x00FF0000) >>> 16;
			int srcAlpha = (srcARGB & 0xFF000000) >>> 24;
			int dstAlpha = (dstARGB & 0xFF000000) >>> 24;

			srcAlpha = (srcAlpha * preAlpha / 255);
			
			// Scale alpha.
			srcBlue =  srcBlue  * srcAlpha / 255;
			srcGreen = srcGreen * srcAlpha / 255;
			srcRed =   srcRed   * srcAlpha / 255;

			int outARGB = 0x00000000;
			outARGB |= Math.min(Math.max(dstBlue  + srcBlue,  0x000), 0x0FF);
			outARGB |= Math.min(Math.max(dstGreen + srcGreen, 0x000), 0x0FF) << 8;
			outARGB |= Math.min(Math.max(dstRed   + srcRed,   0x000), 0x0FF) << 16;
			outARGB |= dstAlpha << 24;
			return outARGB;
		}
	}

	/**
	 * The composite context for {@link SubtractiveComposite}s. 
	 */
	public static class SubtractiveCompositeContext extends ARGBCompositeContext
	{
		private SubtractiveCompositeContext(ColorModel srcColorModel, ColorModel dstColorModel, float preAlpha)
		{
			super(srcColorModel, dstColorModel, preAlpha);
		}

		@Override
		protected int composePixel(int srcARGB, int dstARGB) 
		{
			int srcBlue =  (srcARGB & 0x000000FF);
			int dstBlue =  (dstARGB & 0x000000FF);
			int srcGreen = (srcARGB & 0x0000FF00) >>> 8;
			int dstGreen = (dstARGB & 0x0000FF00) >>> 8;
			int srcRed =   (srcARGB & 0x00FF0000) >>> 16;
			int dstRed =   (dstARGB & 0x00FF0000) >>> 16;
			int srcAlpha = (srcARGB & 0xFF000000) >>> 24;
			int dstAlpha = (dstARGB & 0xFF000000) >>> 24;

			srcAlpha = (srcAlpha * preAlpha / 255);
			
			// Scale alpha.
			srcBlue =  srcBlue  * srcAlpha / 255;
			srcGreen = srcGreen * srcAlpha / 255;
			srcRed =   srcRed   * srcAlpha / 255;

			int outARGB = 0x00000000;
			outARGB |= Math.min(Math.max(dstBlue  - srcBlue,  0x000), 0x0FF);
			outARGB |= Math.min(Math.max(dstGreen - srcGreen, 0x000), 0x0FF) << 8;
			outARGB |= Math.min(Math.max(dstRed   - srcRed,   0x000), 0x0FF) << 16;
			outARGB |= dstAlpha << 24;
			return outARGB;
		}
	}

	/**
	 * The composite context for {@link MultiplicativeComposite}s. 
	 */
	public static class MultiplicativeCompositeContext extends ARGBCompositeContext
	{
		protected MultiplicativeCompositeContext(ColorModel srcColorModel, ColorModel dstColorModel, float preAlpha)
		{
			super(srcColorModel, dstColorModel, preAlpha);
		}

		@Override
		protected int composePixel(int srcARGB, int dstARGB) 
		{
			int srcBlue =  (srcARGB & 0x000000FF);
			int dstBlue =  (dstARGB & 0x000000FF);
			int srcGreen = (srcARGB & 0x0000FF00) >>> 8;
			int dstGreen = (dstARGB & 0x0000FF00) >>> 8;
			int srcRed =   (srcARGB & 0x00FF0000) >>> 16;
			int dstRed =   (dstARGB & 0x00FF0000) >>> 16;
			int srcAlpha = (srcARGB & 0xFF000000) >>> 24;
			int dstAlpha = (dstARGB & 0xFF000000) >>> 24;
			
			srcAlpha = (srcAlpha * preAlpha / 255);
			
			// Scale alpha.
			srcBlue =  srcBlue  + ((255 - srcBlue)  * (255 - srcAlpha) / 255);
			srcGreen = srcGreen + ((255 - srcGreen) * (255 - srcAlpha) / 255);
			srcRed =   srcRed   + ((255 - srcRed)   * (255 - srcAlpha) / 255);

			int outARGB = 0x00000000;
			outARGB |= (dstBlue  * srcBlue  / 255);
			outARGB |= (dstGreen * srcGreen / 255) << 8;
			outARGB |= (dstRed   * srcRed   / 255) << 16;
			outARGB |= dstAlpha << 24;
			return outARGB;
		}
	}

	/**
	 * The composite context for {@link DesaturateComposite}s.
	 * @since 1.10.2.1
	 */
	public static class DesaturationCompositeContext extends ARGBCompositeContext
	{
		protected DesaturationCompositeContext(ColorModel srcColorModel, ColorModel dstColorModel, float preAlpha)
		{
			super(srcColorModel, dstColorModel, preAlpha);
		}

		@Override
		protected int composePixel(int srcARGB, int dstARGB) 
		{
			int srcRed =   (srcARGB & 0x00FF0000) >>> 16;
			int srcAlpha = (srcARGB & 0xFF000000) >>> 24;

			int dstBlue =  (dstARGB & 0x000000FF);
			int dstGreen = (dstARGB & 0x0000FF00) >>> 8;
			int dstRed =   (dstARGB & 0x00FF0000) >>> 16;
			int dstAlpha = (dstARGB & 0xFF000000) >>> 24;
			
			srcAlpha = (srcAlpha * preAlpha / 255);
			
			int dstLum = (dstBlue * 19 / 255) + (dstGreen * 182 / 255) + (dstRed * 54 / 255);
			int srcDesat = srcRed * srcAlpha / 255;
			
			int outARGB = 0x00000000;
			outARGB |= mix(dstBlue,  dstLum, srcDesat);
			outARGB |= mix(dstGreen, dstLum, srcDesat) << 8;
			outARGB |= mix(dstRed,   dstLum, srcDesat) << 16;
			outARGB |= dstAlpha << 24;
			return outARGB;
		}

		private static int mix(int a, int b, int mix)
		{
			return (((255 - mix) * a) + (mix * b)) / 255;
		}
	}
	
}
