package info.laht.acco.render.jme.objects

import com.jme3.asset.AssetManager
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.debug.Arrow
import info.laht.acco.render.jme.getUnshadedMaterial


class JmeAxis(
    length: Float,
    assetManager: AssetManager
) : Node("Axis") {

    init {
        attachChild(
            createArrow(
                assetManager,
                Vector3f.UNIT_X.mult(length),
                ColorRGBA.Red
            )
        )
        attachChild(
            createArrow(
                assetManager,
                Vector3f.UNIT_Y.mult(length),
                ColorRGBA.Green
            )
        )
        attachChild(
            createArrow(
                assetManager,
                Vector3f.UNIT_Z.mult(length),
                ColorRGBA.Blue
            )
        )
    }

    companion object {
        private fun createArrow(
            assetManager: AssetManager,
            extent: Vector3f,
            color: ColorRGBA
        ): Geometry {
            val arrowGeometry = Geometry("", Arrow(extent))
            val material: Material = assetManager.getUnshadedMaterial()
            material.setColor("Color", color)
            arrowGeometry.material = material
            return arrowGeometry
        }
    }

}
