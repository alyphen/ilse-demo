package uk.co.renbinden.ilse.demo.level

import uk.co.renbinden.ilse.asset.ImageAsset
import uk.co.renbinden.ilse.asset.TextAsset
import uk.co.renbinden.ilse.asset.event.AssetLoadEvent
import uk.co.renbinden.ilse.demo.component.Image
import uk.co.renbinden.ilse.demo.component.Position
import uk.co.renbinden.ilse.ecs.Engine
import uk.co.renbinden.ilse.ecs.entity.Entity
import uk.co.renbinden.ilse.ecs.entity.entity
import uk.co.renbinden.ilse.event.Events
import uk.co.renbinden.ilse.event.Listener
import uk.co.renbinden.ilse.tiled.Object
import uk.co.renbinden.ilse.tiled.TiledMapLoader

@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
class LevelLoadListener(
    engine: Engine,
    map: TextAsset,
    getTileSetImage: (String) -> ImageAsset?,
    loadEntity: (Object) -> Entity?
) : Listener<AssetLoadEvent>(handler@{
    val tiledMap = TiledMapLoader.loadMap(map) ?: return@handler
    tiledMap.layers.forEach { layer ->
        layer.data.tiles.chunked(tiledMap.width).forEachIndexed { y, col ->
            col.forEachIndexed { x, tileInstance ->
                engine.add(entity {
                    if (tileInstance != null) {
                        val tileSet = tiledMap.getTileSet(tileInstance)
                        val tileSetColumns = tileSet.columns
                        val tileSetSource = tileSet.image?.source
                        if (tileSetSource != null) {
                            val image = getTileSetImage(tileSetSource)
                            if (image != null && tileSetColumns != null) {
                                add(
                                    Image(
                                        image,
                                        sourceX = (tileInstance.gid - tileSet.firstGid).rem(tileSetColumns) * tileSet.tileWidth,
                                        sourceY = (tileInstance.gid - tileSet.firstGid).div(tileSetColumns) * tileSet.tileHeight,
                                        sourceWidth = tileSet.tileWidth,
                                        sourceHeight = tileSet.tileHeight,
                                        destWidth = tileSet.tileWidth,
                                        destHeight = tileSet.tileHeight
                                    )
                                )
                            }
                        }

                        add(Position((x * tileSet.tileWidth).toDouble(), (y * tileSet.tileHeight).toDouble()))
                    }
                })
            }
        }
    }

    tiledMap.objectGroups.forEach { objectGroup ->
        objectGroup.objects.forEach { obj ->
            val entity = loadEntity(obj)
            if (entity != null) {
                engine.add(entity)
            }
        }
    }
})

@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
fun Engine.loadLevel(map: TextAsset, getTileSetImage: (String) -> ImageAsset?, loadEntity: (Object) -> Entity?) {
    val levelLoadListener = LevelLoadListener(this, map, getTileSetImage, loadEntity)
    Events.listenOnce(AssetLoadEvent, { event -> event.asset == map }, levelLoadListener)
}